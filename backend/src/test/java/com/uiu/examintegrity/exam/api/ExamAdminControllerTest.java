package com.uiu.examintegrity.exam.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.uiu.examintegrity.course.Course;
import com.uiu.examintegrity.course.CourseRepository;
import com.uiu.examintegrity.course.CourseRole;
import com.uiu.examintegrity.course.Enrollment;
import com.uiu.examintegrity.course.EnrollmentRepository;
import com.uiu.examintegrity.exam.persistence.ExamRepository;
import com.uiu.examintegrity.user.AppUserDetails;
import com.uiu.examintegrity.user.Role;
import com.uiu.examintegrity.user.User;
import com.uiu.examintegrity.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExamAdminControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private ExamRepository examRepository;

    private User faculty;
    private User student;
    private Course course;

    @BeforeEach
    void fixtures() {
        faculty = userRepository.save(new User("Import Faculty", "import-fac@uiu.ac.bd", "x", Role.FACULTY));
        student = userRepository.save(new User("Import Student", "import-stu@uiu.ac.bd", "x", Role.STUDENT));
        course = courseRepository.save(new Course("TST 6666", "Import test course"));
        enrollmentRepository.save(new Enrollment(course, faculty, CourseRole.INSTRUCTOR));
        enrollmentRepository.save(new Enrollment(course, student, CourseRole.STUDENT));
    }

    private RequestPostProcessor as(User who) {
        return user(new AppUserDetails(who));
    }

    private MockMultipartHttpServletRequestBuilder upload(Long courseId, byte[] csv) {
        return (MockMultipartHttpServletRequestBuilder) multipart("/api/faculty/exams")
                .file(new MockMultipartFile("file", "exam.csv", "text/csv", csv))
                .param("courseId", String.valueOf(courseId))
                .param("title", "Demo quiz")
                .param("durationMinutes", "3")
                .with(csrf());
    }

    @Test
    @DisplayName("the demo CSV creates a live exam that a student can start immediately")
    void createAndSit() throws Exception {
        byte[] demo = Files.readAllBytes(Path.of("..", "demo", "demo-exam.csv"));
        String body = mockMvc.perform(upload(course.getId(), demo).with(as(faculty)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("LIVE"))
                .andExpect(jsonPath("$.poolSize").value(11))
                .andExpect(jsonPath("$.durationMinutes").value(3))
                .andReturn().getResponse().getContentAsString();
        long examId = ((Number) JsonPath.read(body, "$.id")).longValue();

        mockMvc.perform(post("/api/exams/{id}/sessions", examId).with(csrf()).with(as(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions.length()").value(11))
                .andExpect(jsonPath("$.questions[?(@.type == 'SHORT_ANSWER')].options.length()")
                        .value(org.hamcrest.Matchers.contains(0)))
                .andExpect(jsonPath("$.remainingSeconds").value(org.hamcrest.Matchers.lessThanOrEqualTo(180)));
    }

    @Test
    @DisplayName("a bad file is refused with every line error, and creates nothing")
    void badFile() throws Exception {
        long before = examRepository.count();
        byte[] csv = ("topic,question,option_a,option_b,option_c,option_d,correct,marks\n"
                + "T,Q1?,A,B,C,D,z,1\nT,,A,B,C,D,a,1\n").getBytes();
        mockMvc.perform(upload(course.getId(), csv).with(as(faculty)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.file").value(org.hamcrest.Matchers.containsString("Line 2")))
                .andExpect(jsonPath("$.fieldErrors.file").value(org.hamcrest.Matchers.containsString("Line 3")));
        assertThat(examRepository.count()).isEqualTo(before);
    }

    @Test
    @DisplayName("only staff on the course can create its exams, and students cannot at all")
    void access() throws Exception {
        User other = userRepository.save(new User("Other Fac", "import-other@uiu.ac.bd", "x", Role.FACULTY));
        byte[] csv = "topic,question,option_a,option_b,option_c,option_d,correct\nT,Q?,A,B,C,D,a\n".getBytes();
        mockMvc.perform(upload(course.getId(), csv).with(as(other))).andExpect(status().isNotFound());
        mockMvc.perform(upload(course.getId(), csv).with(as(student))).andExpect(status().isForbidden());
    }
}
