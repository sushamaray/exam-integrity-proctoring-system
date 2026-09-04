package com.uiu.examintegrity.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.uiu.examintegrity.user.Role;
import com.uiu.examintegrity.user.User;
import com.uiu.examintegrity.user.UserRepository;

/**
 * End-to-end checks over the login endpoints.
 *
 * <p>The class is transactional so the account created below is rolled back
 * afterwards and the developer database is left as it was found.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    private static final String EMAIL = "auth-test@uiu.ac.bd";
    private static final String PASSWORD = "correct-horse-battery";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void createAccount() {
        userRepository.save(new User(
                "Auth Test User", EMAIL, passwordEncoder.encode(PASSWORD), Role.FACULTY));
    }

    private String body(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    @Test
    @DisplayName("valid credentials return the user and start a session that /me accepts")
    void loginSucceedsAndSessionPersists() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.role").value("FACULTY"))
                .andExpect(jsonPath("$.fullName").value("Auth Test User"))
                // The password hash must never reach the client.
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        org.junit.jupiter.api.Assertions.assertNotNull(session, "login should have created a session");

        // Reusing the session is the real assertion: it proves AuthService saved
        // the security context, which is the step that is easy to omit.
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    @DisplayName("a wrong password is rejected without revealing that the email exists")
    void loginWithWrongPasswordIsRejected() throws Exception {
        String knownEmail = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(EMAIL, "not-the-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andReturn().getResponse().getContentAsString();

        String unknownEmail = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("nobody@uiu.ac.bd", "not-the-password")))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        // Same message either way, so the response cannot be used to enumerate
        // which addresses have accounts.
        org.junit.jupiter.api.Assertions.assertEquals(
                knownEmail.replaceAll("\"timestamp\":\"[^\"]*\"", ""),
                unknownEmail.replaceAll("\"timestamp\":\"[^\"]*\"", ""));
    }

    @Test
    @DisplayName("/api/auth/me is 401 without a session")
    void meRequiresASession() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a malformed email is rejected with a field error before authentication")
    void loginValidatesInput() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("not-an-email", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }
}
