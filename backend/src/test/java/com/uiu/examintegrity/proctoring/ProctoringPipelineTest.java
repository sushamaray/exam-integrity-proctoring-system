package com.uiu.examintegrity.proctoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.uiu.examintegrity.course.Course;
import com.uiu.examintegrity.course.CourseRepository;
import com.uiu.examintegrity.course.CourseRole;
import com.uiu.examintegrity.course.Enrollment;
import com.uiu.examintegrity.course.EnrollmentRepository;
import com.uiu.examintegrity.exam.application.SessionService;
import com.uiu.examintegrity.exam.persistence.Exam;
import com.uiu.examintegrity.exam.persistence.ExamQuestion;
import com.uiu.examintegrity.exam.persistence.ExamQuestionRepository;
import com.uiu.examintegrity.exam.persistence.ExamRepository;
import com.uiu.examintegrity.exam.persistence.ExamSessionEntity;
import com.uiu.examintegrity.exam.persistence.ExamSessionRepository;
import com.uiu.examintegrity.exam.persistence.ExamStatus;
import com.uiu.examintegrity.proctoring.application.EventsPersisted;
import com.uiu.examintegrity.proctoring.application.RiskAssessment;
import com.uiu.examintegrity.proctoring.application.ScoringService;
import com.uiu.examintegrity.proctoring.application.WindowStore;
import com.uiu.examintegrity.proctoring.domain.RiskTier;
import com.uiu.examintegrity.proctoring.persistence.FlagRepository;
import com.uiu.examintegrity.proctoring.persistence.ProctoringEventRepository;
import com.uiu.examintegrity.proctoring.persistence.RiskScoreRepository;
import com.uiu.examintegrity.question.Question;
import com.uiu.examintegrity.question.QuestionOption;
import com.uiu.examintegrity.question.QuestionOptionRepository;
import com.uiu.examintegrity.question.QuestionRepository;
import com.uiu.examintegrity.question.QuestionType;
import com.uiu.examintegrity.user.AppUserDetails;
import com.uiu.examintegrity.user.Role;
import com.uiu.examintegrity.user.User;
import com.uiu.examintegrity.user.UserRepository;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@RecordApplicationEvents
class ProctoringPipelineTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ApplicationEvents applicationEvents;
    @Autowired private EntityManager entityManager;
    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private QuestionOptionRepository optionRepository;
    @Autowired private ExamRepository examRepository;
    @Autowired private ExamQuestionRepository examQuestionRepository;
    @Autowired private ExamSessionRepository sessionRepository;
    @Autowired private ProctoringEventRepository eventRepository;
    @Autowired private RiskScoreRepository riskScoreRepository;
    @Autowired private FlagRepository flagRepository;
    @Autowired private SessionService sessionService;
    @Autowired private ScoringService scoringService;
    @Autowired private WindowStore windowStore;

    private User student;
    private User faculty;
    private User stranger;
    private Question question;
    private long sessionId;
    private Instant base;

    @BeforeEach
    void fixtures() {
        student = userRepository.save(new User("Pipeline Student", "pipe-student@uiu.ac.bd", "x", Role.STUDENT));
        faculty = userRepository.save(new User("Pipeline Faculty", "pipe-faculty@uiu.ac.bd", "x", Role.FACULTY));
        stranger = userRepository.save(new User("Other Faculty", "pipe-other@uiu.ac.bd", "x", Role.FACULTY));

        Course course = courseRepository.save(new Course("TST 8888", "Pipeline test course"));
        enrollmentRepository.save(new Enrollment(course, student, CourseRole.STUDENT));
        enrollmentRepository.save(new Enrollment(course, faculty, CourseRole.INSTRUCTOR));

        question = questionRepository.save(new Question(course, QuestionType.MCQ, "Pick one", BigDecimal.ONE));
        optionRepository.saveAll(List.of(new QuestionOption(question, "a", "Yes", true),
                new QuestionOption(question, "b", "No", false)));

        Instant now = Instant.now();
        Exam exam = new Exam(course, "Pipeline exam", now.minus(1, ChronoUnit.HOURS), now.plus(2, ChronoUnit.HOURS), 90);
        exam.setStatus(ExamStatus.LIVE);
        exam = examRepository.save(exam);
        examQuestionRepository.save(new ExamQuestion(exam, question, null));

        sessionId = sessionService.startOrResume(exam.getId(), student.getId(), "127.0.0.1", "test").id();

        ExamSessionEntity sitting = sessionRepository.findById(sessionId).orElseThrow();
        sitting.setStartedAt(now.minus(20, ChronoUnit.MINUTES));
        sessionRepository.saveAndFlush(sitting);
        base = now.minus(10, ChronoUnit.MINUTES);
    }

    @AfterEach
    void dropWindow() {
        windowStore.evict(sessionId);
    }

    private RequestPostProcessor as(User who) {
        return user(new AppUserDetails(who));
    }

    private String ev(UUID id, String type, int offsetSeconds, String payload) {
        return """
                {"clientEventId":"%s","type":"%s","occurredAt":"%s","questionId":%d,"payload":%s}"""
                .formatted(id, type, base.plusSeconds(offsetSeconds), question.getId(), payload);
    }

    private String burst() {
        List<String> events = new ArrayList<>();
        events.add(ev(UUID.randomUUID(), "TAB_HIDDEN", 0, "{}"));
        events.add(ev(UUID.randomUUID(), "WINDOW_BLUR", 0, "{}"));
        events.add(ev(UUID.randomUUID(), "TAB_VISIBLE", 20, "{}"));
        events.add(ev(UUID.randomUUID(), "WINDOW_FOCUS", 20, "{}"));
        events.add(ev(UUID.randomUUID(), "PASTE", 22, "{\"charCount\":400}"));
        events.add(ev(UUID.randomUUID(), "FS_EXIT", 30, "{}"));
        return "[" + String.join(",", events) + "]";
    }

    private void ingest(String body) throws Exception {
        mockMvc.perform(post("/api/sessions/{id}/events", sessionId).with(csrf()).with(as(student))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("a batch is stored and announced; the same batch again stores and announces nothing")
    void ingestIsIdempotent() throws Exception {
        String batch = burst();
        ingest(batch);
        ingest(batch);

        assertThat(eventRepository.countBySessionId(sessionId)).isEqualTo(6);
        assertThat(applicationEvents.stream(EventsPersisted.class)).hasSize(1)
                .first().satisfies(e -> assertThat(e.events()).hasSize(6));
    }

    @Test
    @DisplayName("a correlated burst escalates, writes an explainable breakdown, and raises one flag")
    void burstEscalates() throws Exception {
        ingest(burst());
        RiskAssessment assessment = scoringService.rescore(sessionId, List.of());

        assertThat(assessment.tier()).isEqualTo(RiskTier.ESCALATE);
        assertThat(assessment.clusters()).hasSize(1);

        var risk = riskScoreRepository.findById(sessionId).orElseThrow();
        assertThat(risk.getTier()).isEqualTo(RiskTier.ESCALATE);
        assertThat(risk.getBreakdown()).anySatisfy(row -> assertThat(row.get("kind")).isEqualTo("CLUSTER"));
        assertThat(flagRepository.findBySessionId(sessionId)).hasSize(1);

        scoringService.rescore(sessionId, List.of());
        assertThat(flagRepository.findBySessionId(sessionId)).hasSize(1);
    }

    @Test
    @DisplayName("the monitor and the breakdown are visible to the course's staff, and only to them")
    void staffViews() throws Exception {
        ingest(burst());
        scoringService.rescore(sessionId, List.of());
        entityManager.flush();

        mockMvc.perform(get("/api/exams/{id}/monitor",
                        sessionRepository.findById(sessionId).orElseThrow().getExam().getId()).with(as(faculty)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessions[0].sessionId").value(sessionId))
                .andExpect(jsonPath("$.sessions[0].tier").value("ESCALATE"))
                .andExpect(jsonPath("$.sessions[0].flagStatus").value("RAISED"));

        mockMvc.perform(get("/api/sessions/{id}/risk", sessionId).with(as(faculty)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.breakdown[0].kind").value("CLUSTER"))
                .andExpect(jsonPath("$.events[?(@.type == 'PASTE')].hot").value(org.hamcrest.Matchers.contains(true)));

        mockMvc.perform(get("/api/sessions/{id}/risk", sessionId).with(as(stranger)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/sessions/{id}/risk", sessionId).with(as(student)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a reviewer can take a flag, must justify upholding it, and a closed flag stays closed")
    void reviewWorkflow() throws Exception {
        ingest(burst());
        scoringService.rescore(sessionId, List.of());
        entityManager.flush();

        mockMvc.perform(get("/api/faculty/review-queue").with(as(faculty)))
                .andExpect(jsonPath("$[?(@.sessionId == " + sessionId + ")].reason")
                        .value(org.hamcrest.Matchers.contains("CORRELATED_CLUSTER")));

        mockMvc.perform(post("/api/faculty/sessions/{id}/decision", sessionId).with(csrf()).with(as(faculty))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"UNDER_REVIEW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));

        mockMvc.perform(post("/api/faculty/sessions/{id}/decision", sessionId).with(csrf()).with(as(faculty))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"UPHOLD\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/faculty/sessions/{id}/decision", sessionId).with(csrf()).with(as(faculty))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"DISMISS\",\"note\":\"Pasted own notes; allowed.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISMISSED"))
                .andExpect(jsonPath("$.reviewedBy").value("Pipeline Faculty"));

        mockMvc.perform(post("/api/faculty/sessions/{id}/decision", sessionId).with(csrf()).with(as(faculty))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"DISMISS\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("events for a closed or foreign session are refused")
    void refusals() throws Exception {
        mockMvc.perform(post("/api/sessions/{id}/events", sessionId).with(csrf()).with(as(stranger))
                        .contentType(MediaType.APPLICATION_JSON).content(burst()))
                .andExpect(status().isForbidden());

        sessionService.submit(sessionId, student.getId());
        mockMvc.perform(post("/api/sessions/{id}/events", sessionId).with(csrf()).with(as(student))
                        .contentType(MediaType.APPLICATION_JSON).content(burst()))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("an unknown event type is a 400")
    void unknownType() throws Exception {
        mockMvc.perform(post("/api/sessions/{id}/events", sessionId).with(csrf()).with(as(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[" + ev(UUID.randomUUID(), "SCREENSHOT", 0, "{}") + "]"))
                .andExpect(status().isBadRequest());
    }
}
