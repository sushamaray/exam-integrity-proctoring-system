package com.uiu.examintegrity.exam.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.uiu.examintegrity.course.Course;
import com.uiu.examintegrity.course.CourseRepository;
import com.uiu.examintegrity.course.CourseRole;
import com.uiu.examintegrity.course.Enrollment;
import com.uiu.examintegrity.course.EnrollmentRepository;
import com.uiu.examintegrity.exam.application.AutoSubmitScheduler;
import com.uiu.examintegrity.exam.domain.SessionStarted;
import com.uiu.examintegrity.exam.persistence.AnswerRepository;
import com.uiu.examintegrity.exam.persistence.Exam;
import com.uiu.examintegrity.exam.persistence.ExamQuestion;
import com.uiu.examintegrity.exam.persistence.ExamQuestionRepository;
import com.uiu.examintegrity.exam.persistence.ExamRepository;
import com.uiu.examintegrity.exam.persistence.ExamSessionEntity;
import com.uiu.examintegrity.exam.persistence.ExamSessionRepository;
import com.uiu.examintegrity.exam.persistence.ExamStatus;
import com.uiu.examintegrity.exam.persistence.GradingStatus;
import com.uiu.examintegrity.exam.persistence.SessionStatus;
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
class SessionControllerTest {
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
    @Autowired private AnswerRepository answerRepository;
    @Autowired private AutoSubmitScheduler autoSubmitScheduler;

    private User student;
    private User outsider;
    private Exam exam;
    private Question mcq;
    private Question written;
    private String correctLabel;
    private String wrongLabel;

    @BeforeEach
    void fixtures() {
        student = userRepository.save(new User("Session Test", "session-test@uiu.ac.bd", "x", Role.STUDENT));
        outsider = userRepository.save(new User("Other Student", "session-other@uiu.ac.bd", "x", Role.STUDENT));

        Course course = courseRepository.save(new Course("TST 9999", "Session test course"));
        enrollmentRepository.save(new Enrollment(course, student, CourseRole.STUDENT));
        enrollmentRepository.save(new Enrollment(course, outsider, CourseRole.STUDENT));

        mcq = questionRepository.save(new Question(course, QuestionType.MCQ, "Which pattern?", BigDecimal.ONE));
        optionRepository.saveAll(List.of(
                new QuestionOption(mcq, "a", "Observer", true),
                new QuestionOption(mcq, "b", "Adapter", false),
                new QuestionOption(mcq, "c", "Facade", false),
                new QuestionOption(mcq, "d", "Proxy", false)));
        correctLabel = "a";
        wrongLabel = "c";
        written = questionRepository.save(
                new Question(course, QuestionType.SHORT_ANSWER, "Explain LSP.", new BigDecimal("3")));

        Instant now = Instant.now();
        exam = new Exam(course, "Session test exam", now.minus(10, ChronoUnit.MINUTES),
                now.plus(2, ChronoUnit.HOURS), 30);
        exam.setStatus(ExamStatus.LIVE);
        exam = examRepository.save(exam);
        examQuestionRepository.save(new ExamQuestion(exam, mcq, new BigDecimal("2")));
        examQuestionRepository.save(new ExamQuestion(exam, written, null));
    }

    private RequestPostProcessor as(User user) {
        return user(new AppUserDetails(user));
    }

    private long start(User who) throws Exception {
        String body = mockMvc.perform(post("/api/exams/{id}/sessions", exam.getId()).with(csrf()).with(as(who)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String choice(String label) {
        return "{\"optionId\":\"" + label + "\"}";
    }

    @Test
    @DisplayName("starting draws a paper with a server-side clock and no answer key")
    void startReturnsPaper() throws Exception {
        mockMvc.perform(post("/api/exams/{id}/sessions", exam.getId()).with(csrf()).with(as(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.questions.length()").value(2))
                .andExpect(jsonPath("$.remainingSeconds").value(org.hamcrest.Matchers.greaterThan(1790)))
                .andExpect(jsonPath("$.questions[*].options[*].correct").doesNotExist())
                .andExpect(jsonPath("$.questions[*].correct").doesNotExist());
    }

    @Test
    @DisplayName("starting twice resumes the same sitting with the identical paper")
    void startIsIdempotent() throws Exception {
        String first = mockMvc.perform(post("/api/exams/{id}/sessions", exam.getId()).with(csrf()).with(as(student)))
                .andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(post("/api/exams/{id}/sessions", exam.getId()).with(csrf()).with(as(student)))
                .andReturn().getResponse().getContentAsString();

        assertThat((Object) JsonPath.read(second, "$.id")).isEqualTo(JsonPath.read(first, "$.id"));
        assertThat((Object) JsonPath.read(second, "$.questions")).isEqualTo(JsonPath.read(first, "$.questions"));
        assertThat(sessionRepository.findByExamIdAndUserId(exam.getId(), student.getId())).isPresent();

        assertThat(applicationEvents.stream(SessionStarted.class)).hasSize(1);
    }

    @Test
    @DisplayName("autosave is an upsert: a retry, then a change, leave one row with the latest value")
    void autosaveUpserts() throws Exception {
        long sessionId = start(student);

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(put("/api/sessions/{s}/answers/{q}", sessionId, mcq.getId())
                            .with(csrf()).with(as(student))
                            .contentType(MediaType.APPLICATION_JSON).content(choice(wrongLabel)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.savedAt").exists());
        }
        mockMvc.perform(put("/api/sessions/{s}/answers/{q}", sessionId, mcq.getId())
                        .with(csrf()).with(as(student))
                        .contentType(MediaType.APPLICATION_JSON).content(choice(correctLabel)))
                .andExpect(status().isOk());

        assertThat(answerRepository.findBySessionId(sessionId)).hasSize(1);
        mockMvc.perform(get("/api/sessions/{s}", sessionId).with(as(student)))
                .andExpect(jsonPath("$.answers['" + mcq.getId() + "']").value(correctLabel));
    }

    @Test
    @DisplayName("submit grades the MCQ and leaves the written answer for a human")
    void submitGrades() throws Exception {
        long sessionId = start(student);
        mockMvc.perform(put("/api/sessions/{s}/answers/{q}", sessionId, mcq.getId())
                .with(csrf()).with(as(student))
                .contentType(MediaType.APPLICATION_JSON).content(choice(correctLabel)));
        mockMvc.perform(put("/api/sessions/{s}/answers/{q}", sessionId, written.getId())
                        .with(csrf()).with(as(student))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"textAnswer\":\"Subtypes substitute.\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/sessions/{s}/submit", sessionId).with(csrf()).with(as(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("SUBMITTED"));

        entityManager.flush();
        entityManager.clear();
        ExamSessionEntity sitting = sessionRepository.findById(sessionId).orElseThrow();

        assertThat(sitting.getTotalMarks()).isEqualByComparingTo("2");
        assertThat(sitting.getGradingStatus()).isEqualTo(GradingStatus.PARTIAL);
    }

    @Test
    @DisplayName("after submit, a further save and a second submit are refused with 409")
    void closedSessionRefusesWrites() throws Exception {
        long sessionId = start(student);
        mockMvc.perform(post("/api/sessions/{s}/submit", sessionId).with(csrf()).with(as(student)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/sessions/{s}/answers/{q}", sessionId, mcq.getId())
                        .with(csrf()).with(as(student))
                        .contentType(MediaType.APPLICATION_JSON).content(choice(correctLabel)))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/sessions/{s}/submit", sessionId).with(csrf()).with(as(student)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("an answer after the server deadline is refused, and resuming closes the sitting")
    void expiryIsServerAuthoritative() throws Exception {
        long sessionId = start(student);
        expire(sessionId);

        mockMvc.perform(put("/api/sessions/{s}/answers/{q}", sessionId, mcq.getId())
                        .with(csrf()).with(as(student))
                        .contentType(MediaType.APPLICATION_JSON).content(choice(correctLabel)))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/sessions/{s}", sessionId).with(as(student)))
                .andExpect(jsonPath("$.state").value("AUTO_SUBMITTED"))
                .andExpect(jsonPath("$.remainingSeconds").value(0));
    }

    @Test
    @DisplayName("the sweep auto-submits an expired sitting nobody came back to")
    void sweepAutoSubmits() throws Exception {
        long sessionId = start(student);
        expire(sessionId);

        autoSubmitScheduler.closeExpiredSessions();

        entityManager.flush();
        entityManager.clear();
        assertThat(sessionRepository.findById(sessionId).orElseThrow().getStatus())
                .isEqualTo(SessionStatus.AUTO_SUBMITTED);
    }

    @Test
    @DisplayName("somebody else's session is a 404, not a 403")
    void foreignSessionIsNotFound() throws Exception {
        long sessionId = start(student);
        mockMvc.perform(get("/api/sessions/{s}", sessionId).with(as(outsider)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/sessions/{s}/answers/{q}", sessionId, mcq.getId())
                        .with(csrf()).with(as(outsider))
                        .contentType(MediaType.APPLICATION_JSON).content(choice(correctLabel)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a malformed or misdirected answer is a 400")
    void badAnswers() throws Exception {
        long sessionId = start(student);
        mockMvc.perform(put("/api/sessions/{s}/answers/{q}", sessionId, mcq.getId())
                        .with(csrf()).with(as(student))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/sessions/{s}/answers/{q}", sessionId, mcq.getId())
                        .with(csrf()).with(as(student))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"textAnswer\":\"a\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/sessions/{s}/answers/{q}", sessionId, 987654321L)
                        .with(csrf()).with(as(student))
                        .contentType(MediaType.APPLICATION_JSON).content(choice("a")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("the open-exam list shows the exam, and the sitting once started")
    void openExams() throws Exception {
        mockMvc.perform(get("/api/student/exams").with(as(student)))
                .andExpect(jsonPath("$[?(@.id == " + exam.getId() + ")].sessionState").value(
                        org.hamcrest.Matchers.contains((Object) null)));
        start(student);
        mockMvc.perform(get("/api/student/exams").with(as(student)))
                .andExpect(jsonPath("$[?(@.id == " + exam.getId() + ")].sessionState").value(
                        org.hamcrest.Matchers.contains("IN_PROGRESS")));
    }

    @Test
    @DisplayName("staff cannot sit exams")
    void facultyCannotStart() throws Exception {
        User faculty = userRepository.save(new User("Fac", "session-fac@uiu.ac.bd", "x", Role.FACULTY));
        mockMvc.perform(post("/api/exams/{id}/sessions", exam.getId()).with(csrf()).with(as(faculty)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("after submit, the review shows the key, the student's answer and the marks; the list shows the score")
    void reviewAfterSubmit() throws Exception {
        long sessionId = start(student);
        mockMvc.perform(put("/api/sessions/{s}/answers/{q}", sessionId, mcq.getId())
                .with(csrf()).with(as(student))
                .contentType(MediaType.APPLICATION_JSON).content(choice(wrongLabel)));
        mockMvc.perform(post("/api/sessions/{s}/submit", sessionId).with(csrf()).with(as(student)));

        mockMvc.perform(get("/api/sessions/{s}/review", sessionId).with(as(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxMarks").value(5))
                .andExpect(jsonPath("$.totalMarks").value(0))
                .andExpect(jsonPath("$.questions[?(@.id == " + mcq.getId() + ")].correct").value(
                        org.hamcrest.Matchers.contains(correctLabel)))
                .andExpect(jsonPath("$.questions[?(@.id == " + mcq.getId() + ")].yourAnswer").value(
                        org.hamcrest.Matchers.contains(wrongLabel)));

        mockMvc.perform(get("/api/student/sittings").with(as(student)))
                .andExpect(jsonPath("$[0].sessionId").value(sessionId))
                .andExpect(jsonPath("$[0].title").value("Session test exam"));
    }

    @Test
    @DisplayName("no answer key while the exam is still being sat, and none for another student")
    void reviewRefusals() throws Exception {
        long sessionId = start(student);
        mockMvc.perform(get("/api/sessions/{s}/review", sessionId).with(as(student)))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/sessions/{s}/submit", sessionId).with(csrf()).with(as(student)));
        mockMvc.perform(get("/api/sessions/{s}/review", sessionId).with(as(outsider)))
                .andExpect(status().isNotFound());
    }

    private void expire(long sessionId) {
        ExamSessionEntity sitting = sessionRepository.findById(sessionId).orElseThrow();
        sitting.setExpiresAt(Instant.now().minusSeconds(5));
        sessionRepository.saveAndFlush(sitting);
    }
}
