package com.uiu.examintegrity.exam.domain.csv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.uiu.examintegrity.question.QuestionType;

class QuestionCsvParserTest {
    private static final String HEADER = "topic,question,option_a,option_b,option_c,option_d,correct,marks\n";

    private static List<String> errorsOf(String csv) {
        return catchThrowableOfType(CsvImportException.class, () -> QuestionCsvParser.parse(csv)).errors();
    }

    @Test
    @DisplayName("a valid row becomes a question with four options and its marks")
    void validRow() {
        List<ParsedQuestion> parsed = QuestionCsvParser.parse(HEADER + "Generics,What is erasure?,A,B,C,D,b,2\n");
        assertThat(parsed).hasSize(1);
        ParsedQuestion q = parsed.get(0);
        assertThat(q.question()).isEqualTo("What is erasure?");
        assertThat(q.options()).containsExactly("A", "B", "C", "D");
        assertThat(q.correctLabel()).isEqualTo("b");
        assertThat(q.marks()).isEqualByComparingTo("2");
    }

    @Test
    @DisplayName("quoted fields may hold commas, doubled quotes and line breaks; CRLF works")
    void quoting() {
        String csv = HEADER.replace("\n", "\r\n")
                + "Patterns,\"Which pattern, if any, says \"\"notify\"\"?\",Observer,\"Line one\nline two\",C,D,A,\r\n";
        ParsedQuestion q = QuestionCsvParser.parse(csv).get(0);
        assertThat(q.question()).isEqualTo("Which pattern, if any, says \"notify\"?");
        assertThat(q.options().get(1)).isEqualTo("Line one\nline two");
        assertThat(q.correctLabel()).isEqualTo("a");
        assertThat(q.marks()).isEqualByComparingTo("1");
    }

    @Test
    @DisplayName("the marks column may be left off entirely, and blank lines are skipped")
    void optionalMarksAndBlankLines() {
        String csv = "topic,question,option_a,option_b,option_c,option_d,correct\n\nT,Q?,A,B,C,D,d\n\n";
        assertThat(QuestionCsvParser.parse(csv)).hasSize(1);
    }

    @Test
    @DisplayName("a wrong header is refused before any row is read")
    void badHeader() {
        assertThat(errorsOf("question,a,b,c,d,answer\nQ,A,B,C,D,a\n")).singleElement()
                .asString().startsWith("Line 1:");
    }

    @Test
    @DisplayName("every bad row is reported with its line number, not just the first")
    void allErrorsReported() {
        String csv = HEADER
                + "T,Good?,A,B,C,D,a,1\n"
                + "T,,A,B,C,D,a,1\n"
                + "T,Bad answer?,A,B,C,D,e,1\n"
                + "T,Missing option?,A,,C,D,a,1\n"
                + "T,Bad marks?,A,B,C,D,a,lots\n"
                + "T,Too short\n";
        List<String> errors = errorsOf(csv);
        assertThat(errors).hasSize(5);
        assertThat(errors.get(0)).startsWith("Line 3:").contains("question is empty");
        assertThat(errors.get(1)).startsWith("Line 4:").contains("correct must be");
        assertThat(errors.get(2)).startsWith("Line 5:").contains("option_b");
        assertThat(errors.get(3)).startsWith("Line 6:").contains("marks");
        assertThat(errors.get(4)).startsWith("Line 7:").contains("columns");
    }

    @Test
    @DisplayName("an empty file, or a header alone, is refused")
    void empty() {
        assertThat(errorsOf("")).isNotEmpty();
        assertThat(errorsOf(HEADER)).containsExactly("The file has a header but no questions");
    }

    @Test
    @DisplayName("the demo file shipped with the project imports cleanly")
    void demoFile() throws Exception {
        String csv = Files.readString(Path.of("..", "demo", "demo-exam.csv"));
        List<ParsedQuestion> parsed = QuestionCsvParser.parse(csv);
        assertThat(parsed).hasSize(11);
        assertThat(parsed).filteredOn(ParsedQuestion::isWritten).hasSize(1);
        assertThat(parsed).extracting(ParsedQuestion::correctLabel).contains("a", "b", "c", "d");
    }

    @Test
    @DisplayName("options and correct all blank make a written question")
    void writtenRow() {
        ParsedQuestion q = QuestionCsvParser.parse(HEADER + "Patterns,\"Explain, briefly, SRP.\",,,,,,3\n").get(0);
        assertThat(q.type()).isEqualTo(QuestionType.SHORT_ANSWER);
        assertThat(q.options()).isEmpty();
        assertThat(q.correctLabel()).isNull();
        assertThat(q.marks()).isEqualByComparingTo("3");
    }

    @Test
    @DisplayName("a half-filled row is an error, not a written question")
    void halfFilledRow() {
        List<String> errors = errorsOf(HEADER + "T,Q?,A,,,,,1\nT,Q2?,,,,,b,1\n");
        assertThat(errors).anySatisfy(e -> assertThat(e).startsWith("Line 2:").contains("option_b"));
        assertThat(errors).anySatisfy(e -> assertThat(e).startsWith("Line 3:").contains("option_a"));
    }
}
