package com.uiu.examintegrity.exam.infrastructure.code;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.uiu.examintegrity.common.NotFoundException;
import com.uiu.examintegrity.exam.application.view.ConsoleEvent;
import com.uiu.examintegrity.exam.domain.code.Language;

/**
 * The interactive runner against the real toolchains. Each language skips on its own where there
 * is no compiler for it.
 *
 * <p>The three prompt tests are the point of the class: each language buffers output differently
 * on a pipe, and each needs its own trick to make a prompt with no trailing newline appear
 * <em>before</em> the student is expected to answer it. Nothing else checks that.
 */
class InteractiveRunnerTest {
    private static final Long STUDENT = 7L;
    private static final Long OTHER = 8L;
    private static final Long SESSION = 100L;
    private static final String SCOPE = InteractiveRunner.sessionScope(SESSION);

    /** Prompts without a trailing newline, which is the case that only works unbuffered. */
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

    /** The same in C++, opening with the line that defeats an unbuffered {@code stdout} alone. */
    private static final String PROMPTS_CPP = """
            #include <iostream>

            int main() {
                std::ios_base::sync_with_stdio(false);
                int n;
                std::cout << "Enter n: ";
                if (!(std::cin >> n)) {
                    return 1;
                }
                std::cout << "you said " << n << "\\n";
                return 0;
            }
            """;

    /** And in Python, where input() writes its prompt to stdout. */
    private static final String PROMPTS_PYTHON = """
            n = input("Enter n: ")
            print("you said " + n)
            """;

    @TempDir
    private Path workDir;

    private InteractiveRunner runner;
    private CodeExecutionProperties limits;
    private ConsoleProperties console;
    private Toolchains toolchains;
    private final List<ConsoleEvent> events = new CopyOnWriteArrayList<>();

    @BeforeEach
    void runner() {
        limits = new CodeExecutionProperties();
        limits.setWorkDir(workDir.toString());
        limits.setCompileTimeoutMs(30_000);
        toolchains = TestToolchains.over(limits);

        console = new ConsoleProperties();
        runner = new InteractiveRunner(limits, console, toolchains, Clock.systemUTC());
        events.clear();
    }

    @AfterEach
    void cleanUp() {
        runner.liveRunIds().forEach(runId -> runner.stop(runId, "test teardown"));
    }

    private String startAndLaunch(String source) {
        return startAndLaunch(Language.C, source);
    }

    /** Skips rather than fails where this machine has no compiler for the language. */
    private String startAndLaunch(Language language, String source) {
        assumeTrue(toolchains.ready(language), language + " is not installed on this machine");
        String runId = runner.reserve(SCOPE, STUDENT, language, source);
        runner.launch(runId, STUDENT, events::add);
        return runId;
    }

    private String textSoFar() {
        return events.stream()
                .filter(e -> e.type() == ConsoleEvent.Type.STDOUT || e.type() == ConsoleEvent.Type.STDERR)
                .map(ConsoleEvent::text)
                .reduce("", String::concat);
    }

    @Test
    @DisplayName("a prompt with no newline reaches the student before they type -- the whole point")
    void promptArrivesBeforeInput() {
        startAndLaunch(PROMPTS);

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(textSoFar()).contains("Enter n: "));

        // Nothing has been sent yet, so this is genuinely the program waiting on stdin.
        assertThat(events).noneMatch(e -> e.type() == ConsoleEvent.Type.EXITED);
    }

    @Test
    @DisplayName("C++ prompts survive sync_with_stdio(false), which is what unitbuf is for")
    void cppPromptArrivesBeforeInput() {
        String runId = startAndLaunch(Language.CPP, PROMPTS_CPP);

        await().atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> assertThat(textSoFar()).contains("Enter n: "));
        assertThat(events).noneMatch(e -> e.type() == ConsoleEvent.Type.EXITED);

        runner.write(runId, STUDENT, "907", events::add);

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(textSoFar()).contains("you said 907"));
    }

    @Test
    @DisplayName("a Python input() prompt arrives before the answer, thanks to -u")
    void pythonPromptArrivesBeforeInput() {
        String runId = startAndLaunch(Language.PYTHON, PROMPTS_PYTHON);

        await().atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> assertThat(textSoFar()).contains("Enter n: "));
        assertThat(events).noneMatch(e -> e.type() == ConsoleEvent.Type.EXITED);

        runner.write(runId, STUDENT, "907", events::add);

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(textSoFar()).contains("you said 907"));
    }

    @Test
    @DisplayName("a typed line is answered, and the exit code is reported")
    void respondsToTypedInput() {
        String runId = startAndLaunch(PROMPTS);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(textSoFar()).contains("Enter n:"));

        runner.write(runId, STUDENT, "907", events::add);

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(textSoFar()).contains("you said 907"));
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(events)
                .anyMatch(e -> e.type() == ConsoleEvent.Type.EXITED && e.exitCode() == 0));
    }

    @Test
    @DisplayName("source that does not compile never reserves a run")
    void compileFailureReservesNothing() {
        assumeTrue(toolchains.ready(Language.C), "no C compiler on this machine");

        assertThatThrownBy(() -> runner.reserve(SCOPE, STUDENT, Language.C, "int main(void) { return 0 }"))
                .isInstanceOf(InteractiveRunner.CompileFailed.class)
                .hasMessageContaining("expected");

        assertThat(runner.liveCount()).isZero();
    }

    @Test
    @DisplayName("a program that floods stdout is stopped at the cap rather than streaming forever")
    void outputCapStopsAFlood() {
        console.setMaxOutputBytes(4_096);

        startAndLaunch("""
                #include <stdio.h>
                int main(void) {
                    for (;;) { printf("spam spam spam\\n"); }
                    return 0;
                }
                """);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(events)
                .anyMatch(e -> e.type() == ConsoleEvent.Type.ERROR
                        && e.text().contains("printed too much")));
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(runner.liveCount()).isZero());
    }

    @Test
    @DisplayName("a program left sitting at a prompt is reported as expired, with a reason")
    void idleProgramExpires() {
        console.setIdleTimeoutMs(300);
        startAndLaunch(PROMPTS);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(textSoFar()).contains("Enter n:"));
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(runner.expired())
                .extracting(InteractiveRunner.Expired::reason)
                .contains("it sat idle too long"));
    }

    @Test
    @DisplayName("starting a second program in the same scope replaces the first")
    void oneLiveProgramPerScope() {
        startAndLaunch(PROMPTS);
        assertThat(runner.liveCount()).isEqualTo(1);

        startAndLaunch(PROMPTS);
        assertThat(runner.liveCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("another student can neither read, feed nor stop a run")
    void onlyTheOwnerMayTouchARun() {
        String runId = startAndLaunch(PROMPTS);

        assertThat(runner.ownerOf(runId)).contains(STUDENT);
        assertThatThrownBy(() -> runner.write(runId, OTHER, "1", events::add))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> runner.stop(runId, OTHER)).isInstanceOf(NotFoundException.class);
        assertThat(runner.liveCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("closing the exam kills whatever that sitting had running")
    void stoppingAScopeKillsItsProgram() {
        startAndLaunch(PROMPTS);

        runner.stopForScope(SCOPE, "the exam was submitted");

        assertThat(runner.liveCount()).isZero();
    }

    @Test
    @DisplayName("nothing is left in the work directory once a run has ended")
    void workspaceIsCleanedUp() throws Exception {
        String runId = startAndLaunch(PROMPTS);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(textSoFar()).contains("Enter n:"));

        runner.stop(runId, "test");

        try (Stream<Path> leftovers = Files.list(workDir)) {
            assertThat(leftovers).isEmpty();
        }
    }
}
