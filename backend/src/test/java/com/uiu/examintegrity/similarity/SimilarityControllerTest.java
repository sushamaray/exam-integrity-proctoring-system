package com.uiu.examintegrity.similarity;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.uiu.examintegrity.course.Course;
import com.uiu.examintegrity.course.CourseRepository;
import com.uiu.examintegrity.course.CourseRole;
import com.uiu.examintegrity.course.Enrollment;
import com.uiu.examintegrity.course.EnrollmentRepository;
import com.uiu.examintegrity.exam.persistence.AnswerEntity;
import com.uiu.examintegrity.exam.persistence.AnswerRepository;
import com.uiu.examintegrity.exam.persistence.Exam;
import com.uiu.examintegrity.exam.persistence.ExamQuestion;
import com.uiu.examintegrity.exam.persistence.ExamQuestionRepository;
import com.uiu.examintegrity.exam.persistence.ExamRepository;
import com.uiu.examintegrity.exam.persistence.ExamSessionEntity;
import com.uiu.examintegrity.exam.persistence.ExamSessionRepository;
import com.uiu.examintegrity.exam.persistence.ExamStatus;
import com.uiu.examintegrity.exam.persistence.SessionStatus;
import com.uiu.examintegrity.question.Question;
import com.uiu.examintegrity.question.QuestionOption;
import com.uiu.examintegrity.question.QuestionOptionRepository;
import com.uiu.examintegrity.question.QuestionRepository;
import com.uiu.examintegrity.question.QuestionType;
import com.uiu.examintegrity.similarity.persistence.SimilarityPairRepository;
import com.uiu.examintegrity.user.AppUserDetails;
import com.uiu.examintegrity.user.Role;
import com.uiu.examintegrity.user.User;
import com.uiu.examintegrity.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SimilarityControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private QuestionOptionRepository optionRepository;
    @Autowired private ExamRepository examRepository;
    @Autowired private ExamQuestionRepository examQuestionRepository;
    @Autowired private ExamSessionRepository sessionRepository;
    @Autowired private AnswerRepository answerRepository;
    @Autowired private SimilarityPairRepository pairRepository;

    private static final String COPIED = "Inheritance couples the child to its parent, and as a lighthouse"
            + " keeper waving a red lantern would say, composition keeps each part replaceable at runtime.";
    private static final List<String> HONEST = List.of(
            "Composition lets behaviour change at runtime by swapping one collaborator for another object.",
            "Deep hierarchies are hard to follow and every level hides more assumptions from future readers.",
            "Testing is simpler because each composed part can be replaced with a stub in isolation.",
            "Java has single inheritance only, so combining several behaviours needs composition instead.");

    private static final List<String[]> CHOICES = List.of(
            new String[] { "a", "a", "b", "a", "c", "a" },
            new String[] { "a", "c", "a", "a", "a", "d" },
            new String[] { "b", "c", "d", "b", "c", "d" },
            new String[] { "b", "c", "d", "b", "c", "d" },
            new String[] { "c", "d", "b", "d", "b", "c" },
            new String[] { "d", "b", "c", "c", "d", "b" });

    private User faculty;
    private User stranger;
    private Exam exam;
    private final List<ExamSessionEntity> sittings = new ArrayList<>();

    @BeforeEach
    void fixtures() {
        faculty = userRepository.save(new User("Sim Faculty", "sim-faculty@uiu.ac.bd", "x", Role.FACULTY));
        stranger = userRepository.save(new User("Sim Stranger", "sim-stranger@uiu.ac.bd", "x", Role.FACULTY));
        Course course = courseRepository.save(new Course("TST 7777", "Similarity test course"));
        enrollmentRepository.save(new Enrollment(course, faculty, CourseRole.INSTRUCTOR));

        Instant start = Instant.now().minus(3, ChronoUnit.DAYS);
        exam = new Exam(course, "Similarity exam", start, start.plus(2, ChronoUnit.HOURS), 60);
        exam.setStatus(ExamStatus.CLOSED);
        exam = examRepository.save(exam);

        Map<Integer, Map<String, QuestionOption>> options = new HashMap<>();
        List<Question> mcqs = new ArrayList<>();
        for (int q = 0; q < 6; q++) {
            Question question = questionRepository.save(new Question(course, QuestionType.MCQ, "MCQ " + q, BigDecimal.ONE));
            examQuestionRepository.save(new ExamQuestion(exam, question, null));
            Map<String, QuestionOption> byLabel = new HashMap<>();
            for (String label : List.of("a", "b", "c", "d")) {
                byLabel.put(label, optionRepository.save(new QuestionOption(question, label, "Option " + label, label.equals("a"))));
            }
            options.put(q, byLabel);
            mcqs.add(question);
        }
        Question written = questionRepository.save(
                new Question(course, QuestionType.SHORT_ANSWER, "Why prefer composition?", new BigDecimal("3")));
        examQuestionRepository.save(new ExamQuestion(exam, written, null));

        for (int s = 0; s < 6; s++) {
            User student = userRepository.save(new User("Sim Student " + s, "sim-" + s + "@uiu.ac.bd", "x", Role.STUDENT));
            ExamSessionEntity sitting = new ExamSessionEntity(exam, student);
            sitting.setStatus(SessionStatus.SUBMITTED);
            sitting.setStartedAt(start.plusSeconds(60));
            sitting.setExpiresAt(start.plusSeconds(3660));
            sitting.setSubmittedAt(start.plusSeconds(3000));
            sitting = sessionRepository.save(sitting);
            sittings.add(sitting);

            for (int q = 0; q < 6; q++) {
                AnswerEntity answer = new AnswerEntity(sitting, mcqs.get(q));
                answer.setOption(options.get(q).get(CHOICES.get(s)[q]));
                answerRepository.save(answer);
            }
            AnswerEntity text = new AnswerEntity(sitting, written);

            text.setTextAnswer(s < 2 ? COPIED : HONEST.get(s - 2));
            answerRepository.save(text);
        }
    }

    private RequestPostProcessor as(User who) {
        return user(new AppUserDetails(who));
    }

    private String run() throws Exception {
        return mockMvc.perform(post("/api/exams/{id}/similarity/run", exam.getId()).with(csrf()).with(as(faculty)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("both planted pairs are found, by the method that fits each, and nothing else is")
    void findsPlantedPairs() throws Exception {
        String graph = run();

        List<Map<String, Object>> edges = JsonPath.read(graph, "$.edges");
        assertThat(edges).hasSize(2);
        assertThat(edges).anySatisfy(e -> {
            assertThat(e.get("method")).isEqualTo("TFIDF_COSINE");
            assertThat(((Number) e.get("a")).longValue()).isEqualTo(sittings.get(0).getId());
            assertThat(((Number) e.get("b")).longValue()).isEqualTo(sittings.get(1).getId());
        });
        assertThat(edges).anySatisfy(e -> {
            assertThat(e.get("method")).isEqualTo("WRONG_ANSWER");
            assertThat(((Number) e.get("a")).longValue()).isEqualTo(sittings.get(2).getId());
            assertThat(((Number) e.get("b")).longValue()).isEqualTo(sittings.get(3).getId());
        });

        List<Object> clustered = JsonPath.read(graph, "$.nodes[?(@.cluster != null)].sessionId");
        assertThat(clustered).hasSize(4);
    }

    @Test
    @DisplayName("running twice replaces rather than duplicates")
    void rerunIsIdempotent() throws Exception {
        run();
        run();
        assertThat(pairRepository.findByExamIdOrderByScoreDesc(exam.getId())).hasSize(2);
    }

    @Test
    @DisplayName("a pair's detail shows both texts and the matching spans, or the shared mistakes")
    void pairDetail() throws Exception {
        String graph = run();
        Number textPair = JsonPath.<List<Number>>read(graph, "$.edges[?(@.method == 'TFIDF_COSINE')].pairId").get(0);
        Number mcqPair = JsonPath.<List<Number>>read(graph, "$.edges[?(@.method == 'WRONG_ANSWER')].pairId").get(0);

        mockMvc.perform(get("/api/similarity/pairs/{id}", textPair).with(as(faculty)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.a.text").value(COPIED))
                .andExpect(jsonPath("$.spans[0].aStart").value(0));

        mockMvc.perform(get("/api/similarity/pairs/{id}", mcqPair).with(as(faculty)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sharedMistakes.length()").value(6))
                .andExpect(jsonPath("$.sharedMistakes[0].correctOption").value("a"));
    }

    @Test
    @DisplayName("refused while anyone is still sitting the exam, and invisible to other courses' staff")
    void refusals() throws Exception {
        mockMvc.perform(get("/api/exams/{id}/similarity", exam.getId()).with(as(stranger)))
                .andExpect(status().isNotFound());

        ExamSessionEntity open = sittings.get(5);
        open.setStatus(SessionStatus.IN_PROGRESS);
        sessionRepository.saveAndFlush(open);
        mockMvc.perform(post("/api/exams/{id}/similarity/run", exam.getId()).with(csrf()).with(as(faculty)))
                .andExpect(status().isConflict());
    }
}
