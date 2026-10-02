package com.uiu.examintegrity.exam.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

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
import com.uiu.examintegrity.exam.persistence.CodeSubmission;
import com.uiu.examintegrity.exam.persistence.CodeSubmissionRepository;
import com.uiu.examintegrity.exam.persistence.Exam;
import com.uiu.examintegrity.exam.persistence.ExamQuestion;
import com.uiu.examintegrity.exam.persistence.ExamQuestionRepository;
import com.uiu.examintegrity.exam.persistence.ExamRepository;
import com.uiu.examintegrity.exam.persistence.ExamSessionRepository;
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
 * The run endpoints end to end.
 *
 * <p>The authorization and validation tests are deliberately not gated on a compiler, because
 * they must hold on any machine: everything they assert happens before the executor is
 * reached. {@code HIDDEN_EXPECTED} is a distinctive string so a test can assert the whole
 * response body does not contain it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CodeControllerTest {
    private static final String HIDDEN_EXPECTED = "424242";

    private static final String DOUBLE_IT = """
            #include <stdio.h>

            int main(void) {
                int n;
                if (scanf("%d", &n) != 1) {
                    return 1;
                }
                printf("%d\\n", n * 2);
                return 0;
            }
            """;

    private static final String DOUBLE_IT_CPP = """
            #include <iostream>

            int main() {
                int n;
                if (!(std::cin >> n)) {
                    return 1;
                }
                std::cout << n * 2 << std::endl;
                return 0;
            }
            """;

    private static final String DOUBLE_IT_PYTHON = "print(int(input()) * 2)";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private ExamRepository examRepository;
    @Autowired private ExamQuestionRepository examQuestionRepository;
    @Autowired private ExamSessionRepository sessionRepository;
    @Autowired private CodeSubmissionRepository submissionRepository;

    private User student;
    private User outsider;
    private User faculty;
    private Exam exam;
    private Question coding;

    @BeforeEach
    void fixtures() {
        student = userRepository.save(new User("Code Student", "code-student@uiu.ac.bd", "x", Role.STUDENT));
        outsider = userRepository.save(new User("Other Student", "code-other@uiu.ac.bd", "x", Role.STUDENT));
        faculty = userRepository.save(new User("Code Faculty", "code-faculty@uiu.ac.bd", "x", Role.FACULTY));

        Course course = courseRepository.save(new Course("TST 8888", "Code test course"));
        enrollmentRepository.save(new Enrollment(course, student, CourseRole.STUDENT));
        enrollmentRepository.save(new Enrollment(course, outsider, CourseRole.STUDENT));
        enrollmentRepository.save(new Enrollment(course, faculty, CourseRole.INSTRUCTOR));

        coding = new Question(course, QuestionType.CODING, "Print twice the number you read.",
                new BigDecimal("10.00"));
        coding.setStarterCode("#include <stdio.h>\n");
        coding.setTestCases(List.of(
                Map.of("name", "Sample", "input", "21\n", "expected", "42", "hidden", false),
                Map.of("name", "Secret", "input", "212121\n", "expected", HIDDEN_EXPECTED, "hidden", true)));
        coding = questionRepository.save(coding);

        Instant now = Instant.now();
        exam = new Exam(course, "Code test lab", now.minus(5, ChronoUnit.MINUTES),
                now.plus(2, ChronoUnit.HOURS), 45);
        exam.setStatus(ExamStatus.LIVE);
        exam = examRepository.save(exam);
        examQuestionRepository.save(new ExamQuestion(exam, coding, null));
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

    // ---- authorization and validation: must hold with or without a compiler ----

    @Test
    @DisplayName("the exam screen is served the starter code and the sample case, never a hidden one")
    void sessionViewHidesHiddenCases() throws Exception {
        String body = mockMvc.perform(post("/api/exams/{id}/sessions", exam.getId())
                        .with(csrf()).with(as(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].starterCode").value("#include <stdio.h>\n"))
                .andExpect(jsonPath("$.questions[0].tests.length()").value(1))
                .andExpect(jsonPath("$.questions[0].tests[0].name").value("Sample"))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain(HIDDEN_EXPECTED);
    }

    @Test
    @DisplayName("a coding exam is marked so the dashboard sends the student to the editor")
    void openExamsFlagCodingPapers() throws Exception {
        mockMvc.perform(get("/api/student/exams").with(as(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Code test lab"))
                .andExpect(jsonPath("$[0].coding").value(true));
    }

    @Test
    @DisplayName("faculty cannot run code against a student sitting")
    void facultyIsForbidden() throws Exception {
        long sessionId = start(student);

        mockMvc.perform(post("/api/sessions/{s}/code/{q}/run", sessionId, coding.getId())
                        .with(csrf()).with(as(faculty))
                        .contentType(MediaType.APPLICATION_JSON).content(request("C", DOUBLE_IT)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("another student's sitting is reported as missing, not forbidden, so ids stay unguessable")
    void anotherSittingIsNotFound() throws Exception {
        long sessionId = start(student);

        mockMvc.perform(post("/api/sessions/{s}/code/{q}/run", sessionId, coding.getId())
                        .with(csrf()).with(as(outsider))
                        .contentType(MediaType.APPLICATION_JSON).content(request("C", DOUBLE_IT)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a language with no executor is refused with a clear message")
    void unavailableLanguageIsRejected() throws Exception {
        long sessionId = start(student);

        mockMvc.perform(post("/api/sessions/{s}/code/{q}/run", sessionId, coding.getId())
                        .with(csrf()).with(as(student))
                        .contentType(MediaType.APPLICATION_JSON).content(request("JAVA", "class Main {}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not supported yet")));
    }

    @Test
    @DisplayName("source that reaches outside the exam is refused before anything is compiled")
    void guardedSourceIsRejected() throws Exception {
        long sessionId = start(student);

        mockMvc.perform(post("/api/sessions/{s}/code/{q}/run", sessionId, coding.getId())
                        .with(csrf()).with(as(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("C", "int main(void) { system(\"dir\"); }")))
                .andExpect(status().isBadRequest());

        assertThat(submissionRepository.findBySessionIdOrderByCreatedAt(sessionId)).isEmpty();
    }

    @Test
    @DisplayName("the language list is server driven, and reports what this machine can really run")
    void languageListIsServerDriven() throws Exception {
        // Availability is derived from the installed toolchains, so the three built languages are
        // asserted against the same probe the server is effectively using. Java has no toolchain
        // at all, so it is false everywhere and unconditionally.
        mockMvc.perform(get("/api/code/languages").with(as(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].id").value("C"))
                .andExpect(jsonPath("$[0].available").value(ToolchainProbe.hasGcc()))
                .andExpect(jsonPath("$[1].id").value("CPP"))
                .andExpect(jsonPath("$[1].available").value(ToolchainProbe.hasGpp()))
                .andExpect(jsonPath("$[2].id").value("PYTHON"))
                .andExpect(jsonPath("$[2].available").value(ToolchainProbe.hasPython()))
                .andExpect(jsonPath("$[3].id").value("JAVA"))
                .andExpect(jsonPath("$[3].available").value(false));
    }

    @Test
    @DisplayName("a C++ submission is graded against the same cases as the C one")
    void cppSubmissionIsGraded() throws Exception {
        assumeTrue(ToolchainProbe.hasGpp(), "no C++ compiler on this machine");
        long sessionId = start(student);

        mockMvc.perform(post("/api/sessions/{s}/code/{q}/submit", sessionId, coding.getId())
                        .with(csrf()).with(as(student))
                        .contentType(MediaType.APPLICATION_JSON).content(request("CPP", DOUBLE_IT_CPP)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.passed").value(2))
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    @DisplayName("a Python submission is graded too -- the question does not care which language")
    void pythonSubmissionIsGraded() throws Exception {
        assumeTrue(ToolchainProbe.hasPython(), "no Python on this machine");
        long sessionId = start(student);

        mockMvc.perform(post("/api/sessions/{s}/code/{q}/submit", sessionId, coding.getId())
                        .with(csrf()).with(as(student))
                        .contentType(MediaType.APPLICATION_JSON).content(request("PYTHON", DOUBLE_IT_PYTHON)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.passed").value(2))
                .andExpect(jsonPath("$.total").value(2));
    }

    // ---- the real thing, where there is a compiler ----

    @Test
    @DisplayName("Run compiles and reports only the sample cases, and keeps nothing for grading")
    void runUsesSampleCasesOnly() throws Exception {
        assumeTrue(ToolchainProbe.hasGcc(), "no C compiler on this machine");
        long sessionId = start(student);

        mockMvc.perform(post("/api/sessions/{s}/code/{q}/run", sessionId, coding.getId())
                        .with(csrf()).with(as(student))
                        .contentType(MediaType.APPLICATION_JSON).content(request("C", DOUBLE_IT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.isFinal").value(false))
                .andExpect(jsonPath("$.passed").value(1))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.tests[0].stdout").value(org.hamcrest.Matchers.containsString("42")));

        List<CodeSubmission> stored = submissionRepository.findBySessionIdOrderByCreatedAt(sessionId);
        assertThat(stored).hasSize(1);
        assertThat(stored.get(0).isFinal()).isFalse();
    }

    @Test
    @DisplayName("Submit runs the hidden cases too but never returns their output")
    void submitRunsHiddenCasesWithoutLeakingThem() throws Exception {
        assumeTrue(ToolchainProbe.hasGcc(), "no C compiler on this machine");
        long sessionId = start(student);

        String body = mockMvc.perform(post("/api/sessions/{s}/code/{q}/submit", sessionId, coding.getId())
                        .with(csrf()).with(as(student))
                        .contentType(MediaType.APPLICATION_JSON).content(request("C", DOUBLE_IT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFinal").value(true))
                .andExpect(jsonPath("$.passed").value(2))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.tests[1].hidden").value(true))
                .andExpect(jsonPath("$.tests[1].passed").value(true))
                .andExpect(jsonPath("$.tests[1].stdout").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(body)
                .as("a hidden case must not hand the student its expected output")
                .doesNotContain(HIDDEN_EXPECTED);
    }

    @Test
    @DisplayName("a compile error comes back as a result with the diagnostics, not as a 500")
    void compileErrorIsAResult() throws Exception {
        assumeTrue(ToolchainProbe.hasGcc(), "no C compiler on this machine");
        long sessionId = start(student);

        mockMvc.perform(post("/api/sessions/{s}/code/{q}/run", sessionId, coding.getId())
                        .with(csrf()).with(as(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("C", "int main(void) { return 0 }")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPILE_ERROR"))
                .andExpect(jsonPath("$.passed").value(0))
                .andExpect(jsonPath("$.compileOutput").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.blankOrNullString())));
    }

    @Test
    @DisplayName("submitting the exam grades the coding question from the final run, in proportion")
    void examSubmitGradesFromTheFinalRun() throws Exception {
        assumeTrue(ToolchainProbe.hasGcc(), "no C compiler on this machine");
        long sessionId = start(student);

        // Passes the sample but fails the hidden case: 1 of 2, so half of ten marks.
        String halfRight = """
                #include <stdio.h>

                int main(void) {
                    int n;
                    if (scanf("%d", &n) != 1) {
                        return 1;
                    }
                    printf("%d\\n", n == 21 ? 42 : 0);
                    return 0;
                }
                """;
        mockMvc.perform(post("/api/sessions/{s}/code/{q}/submit", sessionId, coding.getId())
                        .with(csrf()).with(as(student))
                        .contentType(MediaType.APPLICATION_JSON).content(request("C", halfRight)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").value(1))
                .andExpect(jsonPath("$.total").value(2));

        mockMvc.perform(post("/api/sessions/{s}/submit", sessionId).with(csrf()).with(as(student)))
                .andExpect(status().isOk());

        assertThat(sessionRepository.findById(sessionId).orElseThrow().getTotalMarks())
                .isEqualByComparingTo("5.00");
    }
}
