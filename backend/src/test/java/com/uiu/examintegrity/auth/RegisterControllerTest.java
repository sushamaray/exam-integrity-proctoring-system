package com.uiu.examintegrity.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.uiu.examintegrity.user.Role;
import com.uiu.examintegrity.user.User;
import com.uiu.examintegrity.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RegisterControllerTest {
    private static final String EMAIL = "new-student@bscse.uiu.ac.bd";
    private static final String STUDENT_ID = "0112219001";
    private static final String PASSWORD = "correct-horse-battery";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private String body(String fullName, String studentId, String email, String password) {
        return """
                {"fullName":"%s","studentId":"%s","email":"%s","password":"%s"}"""
                .formatted(fullName, studentId, email, password);
    }

    @Test
    @DisplayName("registration creates a student and starts a session that /me accepts")
    void registerSucceedsAndSignsIn() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("New Student", STUDENT_ID, EMAIL, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.studentId").value(STUDENT_ID))
                .andExpect(jsonPath("$.role").value("STUDENT"))

                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        Assertions.assertNotNull(session, "registration should have signed the new student in");

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    @DisplayName("a role supplied by the client is ignored; registration always makes a STUDENT")
    void registerCannotEscalateToFaculty() throws Exception {
        String payload = """
                {"fullName":"Sneaky Student","studentId":"%s","email":"%s",
                 "password":"%s","role":"FACULTY","authorities":["ROLE_ADMIN"]}"""
                .formatted(STUDENT_ID, EMAIL, PASSWORD);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STUDENT"));

        User saved = userRepository.findByEmailIgnoreCase(EMAIL).orElseThrow();
        Assertions.assertEquals(Role.STUDENT, saved.getRole(),
                "self-registration must never produce a privileged account");
    }

    @Test
    @DisplayName("a duplicate email is refused with 409")
    void duplicateEmailIsRefused() throws Exception {
        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("First", STUDENT_ID, EMAIL, PASSWORD)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Second", "0112219002", EMAIL, PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("An account with that email already exists"));
    }

    @Test
    @DisplayName("a duplicate student ID is refused with 409")
    void duplicateStudentIdIsRefused() throws Exception {
        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("First", STUDENT_ID, EMAIL, PASSWORD)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Second", STUDENT_ID, "other@bscse.uiu.ac.bd", PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("An account with that student ID already exists"));
    }

    @Test
    @DisplayName("bad input is rejected field by field before anything is written")
    void registerValidatesInput() throws Exception {
        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("", "", "not-an-email", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.fullName").exists())
                .andExpect(jsonPath("$.fieldErrors.studentId").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());

        Assertions.assertTrue(userRepository.findByEmailIgnoreCase("not-an-email").isEmpty());
    }

    @Test
    @DisplayName("registering without a CSRF token is rejected")
    void registerRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("New Student", STUDENT_ID, EMAIL, PASSWORD)))
                .andExpect(status().isForbidden());
    }
}
