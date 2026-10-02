package com.uiu.examintegrity.exam.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.uiu.examintegrity.course.Course;
import com.uiu.examintegrity.course.CourseRepository;
import com.uiu.examintegrity.course.CourseRole;
import com.uiu.examintegrity.course.Enrollment;
import com.uiu.examintegrity.course.EnrollmentRepository;
import com.uiu.examintegrity.exam.infrastructure.code.InteractiveRunner;
import com.uiu.examintegrity.exam.persistence.Exam;
import com.uiu.examintegrity.exam.persistence.ExamQuestion;
import com.uiu.examintegrity.exam.persistence.ExamQuestionRepository;
import com.uiu.examintegrity.exam.persistence.ExamRepository;
import com.uiu.examintegrity.exam.persistence.ExamStatus;
import com.uiu.examintegrity.question.Question;
import com.uiu.examintegrity.question.QuestionRepository;
import com.uiu.examintegrity.question.QuestionType;
import com.uiu.examintegrity.support.ToolchainProbe;
import com.uiu.examintegrity.user.AppUserDetails;
import com.uiu.examintegrity.user.Role;
import com.uiu.examintegrity.user.User;
import com.uiu.examintegrity.user.UserRepository;

/**
 * The console endpoints end to end.
 *
 * <p>Authorization and validation are deliberately not gated on a compiler: they must hold on any
 * machine, and everything they assert happens before anything is built.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConsoleControllerTest {
    private static final String PROMPTS = """
            #include <stdio.h>

            int main(void) {
                int n;
                printf("Enter n: ");
                if (scanf("%d", &n) != 1) {
                    return 1;
                }
                printf("you said %d\\n", n);
                return 0;
            }
            """;

    @Autowired private MockMvc mockMvc;
    @Autowired private InteractiveRunner runner;
    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private ExamRepository examRepository;
    @Autowired private ExamQuestionRepository examQuestionRepository;

    private User student;
    private User outsider;
    private User faculty;
    private Exam exam;
    private Question coding;

    @BeforeEach
    void fixtures() {
        student = userRepository.save(new User("Console Student", "console-student@uiu.ac.bd", "x", Role.STUDENT));
        outsider = userRepository.save(new User("Console Other", "console-other@uiu.ac.bd", "x", Role.STUDENT));
        faculty = userRepository.save(new User("Console Faculty", "console-faculty@uiu.ac.bd", "x", Role.FACULTY));

        Course course = courseRepository.save(new Course("TST 6666", "Console test course"));
        enrollmentRepository.save(new Enrollment(course, student, CourseRole.STUDENT));
        enrollmentRepository.save(new Enrollment(course, outsider, CourseRole.STUDENT));
        enrollmentRepository.save(new Enrollment(course, faculty, CourseRole.INSTRUCTOR));

        coding = new Question(course, QuestionType.CODING, "Print twice the number you read.",
                new BigDecimal("10.00"));
        coding.setStarterCode("#include <stdio.h>\n");
        coding.setTestCases(List.of(
                Map.of("name", "Sample", "input", "21\n", "expected", "42", "hidden", false)));
        coding = questionRepository.save(coding);

        Instant now = Instant.now();
        exam = new Exam(course, "Console test lab", now.minus(5, ChronoUnit.MINUTES),
                now.plus(2, ChronoUnit.HOURS), 45);
        exam.setStatus(ExamStatus.LIVE);
        exam = examRepository.save(exam);
        examQuestionRepository.save(new ExamQuestion(exam, coding, null));
    }

    @AfterEach
    void cleanUp() {
        runner.liveRunIds().forEach(runId -> runner.stop(runId, "test teardown"));
    }

    private RequestPostProcessor as(User who) {
        return user(new AppUserDetails(who));
    }

    private long start(User who) throws Exception {
        String body = mockMvc.perform(post("/api/exams/{id}/sessions", exam.getId()).with(csrf()).with(as(who)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String request(String language, String source) {
        return "{\"language\":\"" + language + "\",\"sourceCode\":" + quote(source) + "}";
    }

    private static String quote(String text) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> out.append(c);
            }
        }
        return out.append('"').toString();
    }

    private String prepare(User who, String source) throws Exception {
        String body = mockMvc.perform(post("/api/sessions/{s}/code/{q}/console", start(who), coding.getId())
                        .with(csrf()).with(as(who))
                        .contentType(MediaType.APPLICATION_JSON).content(request("C", source)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.runId");
    }

    // ---- hold with or without a compiler ----

    @Test
    @DisplayName("faculty cannot start a program against a student sitting")
    void facultyIsForbidden() throws Exception {
        long sessionId = start(student);

        mockMvc.perform(post("/api/sessions/{s}/code/{q}/console", sessionId, coding.getId())
                        .with(csrf()).with(as(faculty))
                        .contentType(MediaType.APPLICATION_JSON).content(request("C", PROMPTS)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("another student's sitting is reported as missing, not forbidden")
    void anotherSittingIsNotFound() throws Exception {
        long sessionId = start(student);

        mockMvc.perform(post("/api/sessions/{s}/code/{q}/console", sessionId, coding.getId())
                        .with(csrf()).with(as(outsider))
                        .contentType(MediaType.APPLICATION_JSON).content(request("C", PROMPTS)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a language with no executor is refused before anything is compiled")
    void unavailableLanguageIsRejected() throws Exception {
        long sessionId = start(student);

        mockMvc.perform(post("/api/sessions/{s}/code/{q}/console", sessionId, coding.getId())
                        .with(csrf()).with(as(student))
                        .contentType(MediaType.APPLICATION_JSON).content(request("JAVA", "class Main {}")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("the source guard applies to a console run too, not just a graded one")
    void guardedSourceIsRejected() throws Exception {
        long sessionId = start(student);

        mockMvc.perform(post("/api/sessions/{s}/code/{q}/console", sessionId, coding.getId())
                        .with(csrf()).with(as(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("C", "int main(void) { system(\"dir\"); }")))
                .andExpect(status().isBadRequest());

        assertThat(runner.liveCount()).isZero();
    }

    @Test
    @DisplayName("an unknown run id is a 404, for its owner and for anybody else")
    void unknownRunIsNotFound() throws Exception {
        long sessionId = start(student);
        String nobodys = java.util.UUID.randomUUID().toString();

        mockMvc.perform(post("/api/sessions/{s}/console/{r}/stdin", sessionId, nobodys)
                        .with(csrf()).with(as(student))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"line\":\"1\"}"))
                .andExpect(status().isNotFound());
    }

    // ---- need a compiler ----

    @Test
    @DisplayName("source that does not build comes back as diagnostics with no run, not as a 500")
    void compileErrorIsReportedWithoutARun() throws Exception {
        assumeTrue(ToolchainProbe.hasGcc(), "no C compiler on this machine");
        long sessionId = start(student);

        mockMvc.perform(post("/api/sessions/{s}/code/{q}/console", sessionId, coding.getId())
                        .with(csrf()).with(as(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("C", "int main(void) { return 0 }")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").doesNotExist())
                .andExpect(jsonPath("$.compileOutput").value(org.hamcrest.Matchers.containsString("expected")));

        assertThat(runner.liveCount()).isZero();
    }

    @Test
    @DisplayName("prepare reserves a run owned by the student, which is what authorizes the subscription")
    void prepareReservesAnOwnedRun() throws Exception {
        assumeTrue(ToolchainProbe.hasGcc(), "no C compiler on this machine");

        String runId = prepare(student, PROMPTS);

        assertThat(runId).isNotBlank();
        assertThat(runner.ownerOf(runId)).contains(student.getId());
    }

    @Test
    @DisplayName("start, type a line, then stop -- the whole loop over HTTP")
    void fullLifecycle() throws Exception {
        assumeTrue(ToolchainProbe.hasGcc(), "no C compiler on this machine");
        long sessionId = start(student);
        String runId = prepare(student, PROMPTS);

        mockMvc.perform(post("/api/sessions/{s}/console/{r}/start", sessionId, runId)
                        .with(csrf()).with(as(student)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/sessions/{s}/console/{r}/stdin", sessionId, runId)
                        .with(csrf()).with(as(student))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"line\":\"907\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/sessions/{s}/console/{r}", sessionId, runId)
                        .with(csrf()).with(as(student)))
                .andExpect(status().isNoContent());

        assertThat(runner.liveCount()).isZero();
    }

    @Test
    @DisplayName("a run cannot be launched twice")
    void launchingTwiceIsAConflict() throws Exception {
        assumeTrue(ToolchainProbe.hasGcc(), "no C compiler on this machine");
        long sessionId = start(student);
        String runId = prepare(student, PROMPTS);

        mockMvc.perform(post("/api/sessions/{s}/console/{r}/start", sessionId, runId)
                        .with(csrf()).with(as(student)))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/sessions/{s}/console/{r}/start", sessionId, runId)
                        .with(csrf()).with(as(student)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("another student cannot feed or stop a run that is not theirs")
    void anotherStudentCannotDriveTheRun() throws Exception {
        assumeTrue(ToolchainProbe.hasGcc(), "no C compiler on this machine");
        long sessionId = start(student);
        String runId = prepare(student, PROMPTS);

        mockMvc.perform(post("/api/sessions/{s}/console/{r}/stdin", sessionId, runId)
                        .with(csrf()).with(as(outsider))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"line\":\"907\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/sessions/{s}/console/{r}", sessionId, runId)
                        .with(csrf()).with(as(outsider)))
                .andExpect(status().isNotFound());

        assertThat(runner.liveCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("submitting the exam kills a console that is still open")
    void submittingTheExamKillsTheConsole() throws Exception {
        assumeTrue(ToolchainProbe.hasGcc(), "no C compiler on this machine");
        long sessionId = start(student);
        String runId = prepare(student, PROMPTS);

        mockMvc.perform(post("/api/sessions/{s}/console/{r}/start", sessionId, runId)
                        .with(csrf()).with(as(student)))
                .andExpect(status().isNoContent());
        assertThat(runner.liveCount()).isEqualTo(1);

        mockMvc.perform(post("/api/sessions/{s}/submit", sessionId).with(csrf()).with(as(student)))
                .andExpect(status().isOk());

        assertThat(runner.liveCount())
                .as("a program must not outlive its exam")
                .isZero();
    }
}
