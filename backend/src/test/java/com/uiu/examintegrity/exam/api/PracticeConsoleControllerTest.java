package com.uiu.examintegrity.exam.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.uiu.examintegrity.exam.infrastructure.code.InteractiveRunner;
import com.uiu.examintegrity.support.ToolchainProbe;
import com.uiu.examintegrity.user.AppUserDetails;
import com.uiu.examintegrity.user.Role;
import com.uiu.examintegrity.user.User;
import com.uiu.examintegrity.user.UserRepository;

/**
 * The compiler outside any exam.
 *
 * <p>This is the widest surface for running arbitrary code in the app -- no exam, no window, and
 * open to faculty too -- so what it refuses matters as much as what it allows.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PracticeConsoleControllerTest {
    private static final String HELLO = """
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

    private User student;
    private User faculty;
    private User otherStudent;

    @BeforeEach
    void fixtures() {
        student = userRepository.save(new User("Lab Student", "lab-student@uiu.ac.bd", "x", Role.STUDENT));
        otherStudent = userRepository.save(new User("Lab Other", "lab-other@uiu.ac.bd", "x", Role.STUDENT));
        faculty = userRepository.save(new User("Lab Faculty", "lab-faculty@uiu.ac.bd", "x", Role.FACULTY));
    }

    @AfterEach
    void cleanUp() {
        runner.liveRunIds().forEach(runId -> runner.stop(runId, "test teardown"));
    }

    private RequestPostProcessor as(User who) {
        return user(new AppUserDetails(who));
    }

    private static String request(String language, String source) {
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

    private String prepare(User who) throws Exception {
        String body = mockMvc.perform(post("/api/code/console").with(csrf()).with(as(who))
                        .contentType(MediaType.APPLICATION_JSON).content(request("C", HELLO)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.runId");
    }

    // ---- hold with or without a compiler ----

    @Test
    @DisplayName("signing out is the only thing that keeps you out; both roles may compile")
    void bothRolesAreAllowed() throws Exception {
        mockMvc.perform(post("/api/code/console").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(request("C", HELLO)))
                .andExpect(status().isUnauthorized());

        // A 400 would mean it got past security and failed validation instead, which is not the
        // point here, so these assert only that neither role is refused outright.
        for (User who : new User[] { student, faculty }) {
            mockMvc.perform(post("/api/code/console").with(csrf()).with(as(who))
                            .contentType(MediaType.APPLICATION_JSON).content(request("JAVA", "class Main {}")))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    @DisplayName("the source guard applies with no exam behind it, where it matters most")
    void guardAppliesToPractice() throws Exception {
        mockMvc.perform(post("/api/code/console").with(csrf()).with(as(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("C", "int main(void) { system(\"dir\"); }")))
                .andExpect(status().isBadRequest());

        assertThat(runner.liveCount()).isZero();
    }

    @Test
    @DisplayName("an unknown run is a 404")
    void unknownRunIsNotFound() throws Exception {
        mockMvc.perform(post("/api/code/console/{r}/stdin", java.util.UUID.randomUUID())
                        .with(csrf()).with(as(student))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"line\":\"1\"}"))
                .andExpect(status().isNotFound());
    }

    // ---- need a compiler ----

    @Test
    @DisplayName("code that does not build returns diagnostics and reserves nothing")
    void compileErrorReservesNothing() throws Exception {
        assumeTrue(ToolchainProbe.hasGcc(), "no C compiler on this machine");

        mockMvc.perform(post("/api/code/console").with(csrf()).with(as(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("C", "int main(void) { return 0 }")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").doesNotExist())
                .andExpect(jsonPath("$.compileOutput").value(org.hamcrest.Matchers.containsString("expected")));

        assertThat(runner.liveCount()).isZero();
    }

    @Test
    @DisplayName("faculty can run code with no exam in sight")
    void facultyCanUseTheLab() throws Exception {
        assumeTrue(ToolchainProbe.hasGcc(), "no C compiler on this machine");

        String runId = prepare(faculty);

        assertThat(runId).isNotBlank();
        assertThat(runner.ownerOf(runId)).contains(faculty.getId());
    }

    @Test
    @DisplayName("start, type a line, stop")
    void fullLifecycle() throws Exception {
        assumeTrue(ToolchainProbe.hasGcc(), "no C compiler on this machine");
        String runId = prepare(student);

        mockMvc.perform(post("/api/code/console/{r}/start", runId).with(csrf()).with(as(student)))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/code/console/{r}/stdin", runId).with(csrf()).with(as(student))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"line\":\"907\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/code/console/{r}", runId).with(csrf()).with(as(student)))
                .andExpect(status().isNoContent());

        assertThat(runner.liveCount()).isZero();
    }

    @Test
    @DisplayName("one live program per person, so a second run replaces the first")
    void oneProgramPerPerson() throws Exception {
        assumeTrue(ToolchainProbe.hasGcc(), "no C compiler on this machine");

        prepare(student);
        assertThat(runner.liveCount()).isEqualTo(1);
        prepare(student);
        assertThat(runner.liveCount()).isEqualTo(1);

        // Somebody else's run is their own and must not be displaced by it.
        prepare(faculty);
        assertThat(runner.liveCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("somebody else cannot drive your scratchpad program")
    void runsAreNotShared() throws Exception {
        assumeTrue(ToolchainProbe.hasGcc(), "no C compiler on this machine");
        String runId = prepare(student);

        mockMvc.perform(post("/api/code/console/{r}/stdin", runId).with(csrf()).with(as(otherStudent))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"line\":\"1\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/code/console/{r}", runId).with(csrf()).with(as(faculty)))
                .andExpect(status().isNotFound());

        assertThat(runner.liveCount()).isEqualTo(1);
    }
}
