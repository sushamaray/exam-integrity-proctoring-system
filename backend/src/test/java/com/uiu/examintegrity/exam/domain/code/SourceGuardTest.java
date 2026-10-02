package com.uiu.examintegrity.exam.domain.code;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SourceGuardTest {
    private static final String SOLUTION = """
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

    private static final String SOLUTION_CPP = """
            #include <iostream>
            #include <vector>

            int main() {
                int n;
                std::cin >> n;
                std::vector<int> all(n, 0);
                std::cout << all.size() << std::endl;
                return 0;
            }
            """;

    private static final String SOLUTION_PYTHON = """
            import sys

            for line in sys.stdin:
                print(line.strip().upper())
            """;

    @Test
    @DisplayName("an ordinary answer is accepted")
    void acceptsOrdinaryCode() {
        assertThat(SourceGuard.reject(Language.C, SOLUTION)).isEmpty();
    }

    @Test
    @DisplayName("an ordinary C++ answer is accepted, iostream and all")
    void acceptsOrdinaryCpp() {
        assertThat(SourceGuard.reject(Language.CPP, SOLUTION_CPP)).isEmpty();
    }

    @Test
    @DisplayName("an ordinary Python answer is accepted, and sys.stdin with it")
    void acceptsOrdinaryPython() {
        assertThat(SourceGuard.reject(Language.PYTHON, "print(int(input()) * 2)")).isEmpty();
        assertThat(SourceGuard.reject(Language.PYTHON, SOLUTION_PYTHON)).isEmpty();
    }

    @ParameterizedTest
    @DisplayName("code that reaches outside the exam is refused")
    @ValueSource(strings = {
            "#include <windows.h>\nint main(void) { return 0; }",
            "#include <winsock2.h>\nint main(void) { return 0; }",
            "int main(void) { system(\"dir\"); }",
            "int main(void) { FILE *f = fopen(\"secrets.txt\", \"r\"); }",
            "int main(void) { remove(\"grades.db\"); }",
            "int main(void) { int s = socket(2, 1, 0); }",
            "int main(void) { __asm__(\"nop\"); }",
    })
    void refusesEscapes(String source) {
        assertThat(SourceGuard.reject(Language.C, source)).isPresent();
    }

    @ParameterizedTest
    @DisplayName("C++ gets the C denylist plus the file handling that is only C++'s")
    @ValueSource(strings = {
            "#include <fstream>\nint main() { std::ifstream in(\"x\"); }",
            "#include <filesystem>\nint main() { return 0; }",
            "int main() { std::system(\"dir\"); }",
    })
    void refusesCppEscapes(String source) {
        assertThat(SourceGuard.reject(Language.CPP, source)).isPresent();
    }

    @ParameterizedTest
    @DisplayName("Python that reaches outside the exam is refused, mostly at the import")
    @ValueSource(strings = {
            "import os\nprint(os.listdir('.'))",
            "from os import listdir",
            "import subprocess\nsubprocess.run(['dir'])",
            "import socket\ns = socket.socket()",
            "import shutil",
            "import ctypes",
            "data = open('secrets.txt').read()",
            "__import__('os').system('dir')",
            "eval(input())",
    })
    void refusesPythonEscapes(String source) {
        assertThat(SourceGuard.reject(Language.PYTHON, source)).isPresent();
    }

    @Test
    @DisplayName("the lists do not bleed between languages")
    void listsAreSeparate() {
        // open( is a Python escape and fopen( is a C one. Sharing one list would mean either
        // refusing every C answer that opens a file by name, or letting Python open() through.
        assertThat(SourceGuard.reject(Language.C, "int main(void) { int open = 1; return open; }")).isEmpty();
        assertThat(SourceGuard.reject(Language.PYTHON, "print('fork(')")).isEmpty();
    }

    @Test
    @DisplayName("the denylist is case insensitive, so SYSTEM( does not slip past system(")
    void caseInsensitive() {
        assertThat(SourceGuard.reject(Language.C, "int main(void) { SYSTEM(\"dir\"); }")).isPresent();
    }

    @Test
    @DisplayName("an empty submission is refused with its own message, not a compiler error")
    void refusesEmpty() {
        assertThat(SourceGuard.reject(Language.C, "   ")).contains("There is no code to run");
    }

    @Test
    @DisplayName("languages parse by name, case insensitively, and unknown ones are not guessed at")
    void languagesParseByName() {
        assertThat(Language.parse("c")).contains(Language.C);
        assertThat(Language.parse(" CPP ")).contains(Language.CPP);
        assertThat(Language.parse("python")).contains(Language.PYTHON);
        assertThat(Language.parse("rust")).isEmpty();
        assertThat(Language.parse(null)).isEmpty();
    }

    @Test
    @DisplayName("trailing whitespace and CRLF are not wrong answers")
    void outputComparisonIgnoresLineEndings() {
        assertThat(TestOutcome.matches("42\r\n", "42")).isTrue();
        assertThat(TestOutcome.matches("42   \n", "42")).isTrue();
        assertThat(TestOutcome.matches("1\r\n2\r\n", "1\n2")).isTrue();
        assertThat(TestOutcome.matches("42", "4 2")).isFalse();
        assertThat(TestOutcome.matches("", "0")).isFalse();
    }

    @Test
    @DisplayName("a hidden case is parsed as hidden and kept out of the visible set")
    void hiddenCasesAreSeparated() {
        CodingProblem problem = CodingProblem.from(7L, "int main(void) {}", java.util.List.of(
                java.util.Map.of("name", "Sample", "input", "21", "expected", "42", "hidden", false),
                java.util.Map.of("input", "212121", "expected", "424242", "hidden", true)));

        assertThat(problem.tests()).hasSize(2);
        assertThat(problem.visibleTests()).hasSize(1);
        assertThat(problem.visibleTests().get(0).name()).isEqualTo("Sample");
        assertThat(problem.tests().get(1).name()).isEqualTo("Hidden case 2");
        assertThat(problem.runnable()).isTrue();
    }
}
