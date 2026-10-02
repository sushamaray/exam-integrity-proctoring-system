package com.uiu.examintegrity.exam.infrastructure.code;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.uiu.examintegrity.common.ConflictException;
import com.uiu.examintegrity.exam.domain.code.CodeRun;
import com.uiu.examintegrity.exam.domain.code.Language;
import com.uiu.examintegrity.exam.domain.code.RunOutcome;
import com.uiu.examintegrity.exam.domain.code.RunStatus;
import com.uiu.examintegrity.exam.domain.code.TestCase;
import com.uiu.examintegrity.support.ToolchainProbe;

/**
 * Exercises the real toolchains. Each language is skipped on its own where there is no compiler
 * for it, so a machine with gcc but no Python still proves what it can.
 */
class LocalProcessExecutorTest {
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

    private static final String DOUBLE_IT_PYTHON = """
            n = int(input())
            print(n * 2)
            """;

    private static String gcc;

    @TempDir
    private Path workDir;

    private CodeExecutionProperties limits;
    private Toolchains toolchains;

    @BeforeAll
    static void toolchain() {
        gcc = ToolchainProbe.gcc();
    }

    @BeforeEach
    void limits() {
        limits = new CodeExecutionProperties();
        limits.setWorkDir(workDir.toString());
        limits.setRunTimeoutMs(2_000);
        limits.setCompileTimeoutMs(30_000);
        toolchains = TestToolchains.over(limits);
    }

    /** The C cases, which are most of them, read better without an assumption line each. */
    private RunOutcome run(String source, TestCase... tests) {
        assumeTrue(gcc != null, "no C compiler on this machine");
        return run(Language.C, source, tests);
    }

    private RunOutcome run(Language language, String source, TestCase... tests) {
        return new LocalProcessExecutor(limits, toolchains).run(new CodeRun(language, source, List.of(tests)));
    }

    private static Stream<Arguments> doublers() {
        return Stream.of(
                Arguments.of(Language.C, DOUBLE_IT),
                Arguments.of(Language.CPP, DOUBLE_IT_CPP),
                Arguments.of(Language.PYTHON, DOUBLE_IT_PYTHON));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("doublers")
    @DisplayName("a correct program passes in every language, reading stdin and writing stdout")
    void correctProgramPasses(Language language, String source) {
        assumeTrue(toolchains.ready(language), language + " is not installed on this machine");

        RunOutcome outcome = run(language, source,
                new TestCase("Sample", "21\n", "42", false),
                new TestCase("Hidden", "212121\n", "424242", true));

        assertThat(outcome.status()).isEqualTo(RunStatus.OK);
        assertThat(outcome.passedCount()).isEqualTo(2);
        assertThat(outcome.totalCount()).isEqualTo(2);
        assertThat(outcome.tests().get(0).stdout().strip()).isEqualTo("42");
    }

    @Test
    @DisplayName("a wrong answer is a failure, not an error -- the program ran fine")
    void wrongAnswerStillRan() {
        RunOutcome outcome = run(DOUBLE_IT, new TestCase("Sample", "21\n", "43", false));

        assertThat(outcome.status()).isEqualTo(RunStatus.OK);
        assertThat(outcome.passedCount()).isZero();
        assertThat(outcome.tests().get(0).status()).isEqualTo(RunStatus.OK);
        assertThat(outcome.tests().get(0).passed()).isFalse();
    }

    @Test
    @DisplayName("a syntax error reports the compiler's own diagnostics and runs nothing")
    void compileErrorCarriesDiagnostics() {
        RunOutcome outcome = run("int main(void) { return 0 }",
                new TestCase("Sample", "", "", false));

        assertThat(outcome.status()).isEqualTo(RunStatus.COMPILE_ERROR);
        assertThat(outcome.tests()).isEmpty();
        assertThat(outcome.compileOutput()).containsIgnoringCase("expected");
    }

    @Test
    @DisplayName("a Python syntax error is a compile error too, not a crash at run time")
    void pythonSyntaxErrorIsACompileError() {
        assumeTrue(toolchains.ready(Language.PYTHON), "no Python on this machine");

        RunOutcome outcome = run(Language.PYTHON, "print(int(input())\n",
                new TestCase("Sample", "21\n", "42", false));

        assertThat(outcome.status()).isEqualTo(RunStatus.COMPILE_ERROR);
        assertThat(outcome.tests()).isEmpty();
        assertThat(outcome.compileOutput()).containsIgnoringCase("syntax");
    }

    @Test
    @DisplayName("a Python traceback names the script, never a directory on the server")
    void pythonTracebackDoesNotLeakServerPaths() {
        assumeTrue(toolchains.ready(Language.PYTHON), "no Python on this machine");

        RunOutcome outcome = run(Language.PYTHON, "raise ValueError(\"nope\")\n",
                new TestCase("Sample", "", "", false));

        assertThat(outcome.tests().get(0).status()).isEqualTo(RunStatus.RUNTIME_ERROR);
        assertThat(outcome.tests().get(0).stderr())
                .contains("main.py")
                .doesNotContain(workDir.toAbsolutePath().toString());
    }

    @Test
    @DisplayName("the compiler's diagnostics never name a path on the server")
    void diagnosticsDoNotLeakServerPaths() {
        RunOutcome outcome = run("int main(void) { return 0 }", new TestCase("Sample", "", "", false));

        assertThat(outcome.compileOutput()).doesNotContain(workDir.toAbsolutePath().toString());
    }

    @Test
    @DisplayName("an endless loop is stopped at the timeout instead of hanging the request")
    void infiniteLoopTimesOut() {
        RunOutcome outcome = run("""
                int main(void) {
                    while (1) {
                    }
                    return 0;
                }
                """, new TestCase("Sample", "", "never", false));

        assertThat(outcome.status()).isEqualTo(RunStatus.TIMEOUT);
        assertThat(outcome.tests().get(0).passed()).isFalse();
        assertThat(outcome.tests().get(0).runtimeMs()).isLessThan(15_000);
    }

    @Test
    @DisplayName("a program that floods stdout is cut off at the cap")
    void floodingOutputHitsTheCap() {
        limits.setMaxOutputBytes(1_024);

        RunOutcome outcome = run("""
                #include <stdio.h>

                int main(void) {
                    for (int i = 0; i < 20000; i++) {
                        printf("spam ");
                    }
                    return 0;
                }
                """, new TestCase("Sample", "", "spam", false));

        assertThat(outcome.status()).isEqualTo(RunStatus.OUTPUT_LIMIT);
        assertThat(outcome.tests().get(0).passed()).isFalse();
        assertThat(outcome.tests().get(0).stdout()).hasSizeLessThan(2_048);
    }

    @Test
    @DisplayName("a non-zero exit is a runtime error")
    void nonZeroExitIsARuntimeError() {
        RunOutcome outcome = run("int main(void) { return 3; }", new TestCase("Sample", "", "", false));

        assertThat(outcome.status()).isEqualTo(RunStatus.RUNTIME_ERROR);
    }

    @Test
    @DisplayName("the throwaway directory is gone afterwards, however the run ended")
    void workspaceIsCleanedUp() throws IOException {
        run(DOUBLE_IT, new TestCase("Sample", "21\n", "42", false));
        run("int main(void) { return 0 }", new TestCase("Sample", "", "", false));

        try (Stream<Path> leftovers = Files.list(workDir)) {
            assertThat(leftovers).isEmpty();
        }
    }

    @Test
    @DisplayName("Python leaves no __pycache__ behind either")
    void pythonWorkspaceIsCleanedUp() throws IOException {
        assumeTrue(toolchains.ready(Language.PYTHON), "no Python on this machine");

        run(Language.PYTHON, DOUBLE_IT_PYTHON, new TestCase("Sample", "21\n", "42", false));

        try (Stream<Path> leftovers = Files.list(workDir)) {
            assertThat(leftovers).isEmpty();
        }
    }

    @Test
    @DisplayName("with every slot taken a run is refused as a conflict rather than queued forever")
    void refusesWhenSaturated() throws InterruptedException {
        assumeTrue(gcc != null, "no C compiler on this machine");
        limits.setMaxConcurrentRuns(1);
        limits.setQueueTimeoutMs(100);
        LocalProcessExecutor executor = new LocalProcessExecutor(limits, toolchains);

        CodeRun endless = new CodeRun(Language.C, """
                int main(void) {
                    while (1) {
                    }
                    return 0;
                }
                """, List.of(new TestCase("Sample", "", "never", false)));

        Thread hog = new Thread(() -> executor.run(endless));
        hog.start();
        Thread.sleep(600);

        assertThatThrownBy(() -> executor.run(
                new CodeRun(Language.C, DOUBLE_IT, List.of(new TestCase("Sample", "21\n", "42", false)))))
                .isInstanceOf(ConflictException.class);

        hog.join();
    }

    @Test
    @DisplayName("what is claimed is what is installed, and Java is claimed by nobody")
    void claimsWhatIsInstalled() {
        LocalProcessExecutor executor = new LocalProcessExecutor(limits, toolchains);

        assertThat(executor.supports(Language.C)).isEqualTo(ToolchainProbe.hasGcc());
        assertThat(executor.supports(Language.CPP)).isEqualTo(ToolchainProbe.hasGpp());
        assertThat(executor.supports(Language.PYTHON)).isEqualTo(ToolchainProbe.hasPython());
        assertThat(executor.supports(Language.JAVA)).isFalse();
    }
}
