package com.uiu.examintegrity.exam.domain.csv;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.uiu.examintegrity.question.QuestionType;

public final class QuestionCsvParser {
    public static final List<String> HEADER = List.of(
            "topic", "question", "option_a", "option_b", "option_c", "option_d", "correct", "marks");
    public static final int MAX_QUESTIONS = 200;

    private static final Set<String> LABELS = Set.of("a", "b", "c", "d");

    private QuestionCsvParser() {
    }

    public static List<ParsedQuestion> parse(String content) {
        List<Row> rows = tokenize(content.startsWith("﻿") ? content.substring(1) : content);
        List<String> errors = new ArrayList<>();

        if (rows.isEmpty()) {
            throw new CsvImportException(List.of("The file is empty"));
        }
        List<String> header = rows.get(0).fields().stream()
                .map(h -> h.strip().toLowerCase(Locale.ROOT)).toList();
        if (!header.equals(HEADER) && !header.equals(HEADER.subList(0, 7))) {
            throw new CsvImportException(List.of(
                    "Line 1: the header must be " + String.join(",", HEADER)));
        }

        List<ParsedQuestion> questions = new ArrayList<>();
        for (Row row : rows.subList(1, rows.size())) {
            if (row.fields().stream().allMatch(String::isBlank)) {
                continue;
            }
            ParsedQuestion parsed = parseRow(row, errors);
            if (parsed != null) {
                questions.add(parsed);
            }
        }

        if (questions.isEmpty() && errors.isEmpty()) {
            errors.add("The file has a header but no questions");
        }
        if (questions.size() > MAX_QUESTIONS) {
            errors.add("At most " + MAX_QUESTIONS + " questions per file, found " + questions.size());
        }
        if (!errors.isEmpty()) {
            throw new CsvImportException(errors);
        }
        return questions;
    }

    private static ParsedQuestion parseRow(Row row, List<String> errors) {
        String at = "Line " + row.line() + ": ";
        List<String> f = row.fields();
        if (f.size() < 7 || f.size() > 8) {
            errors.add(at + "expected 7 or 8 columns, found " + f.size());
            return null;
        }

        int before = errors.size();
        String topic = f.get(0).strip();
        String question = f.get(1).strip();
        List<String> options = f.subList(2, 6).stream().map(String::strip).toList();
        String correct = f.get(6).strip().toLowerCase(Locale.ROOT);
        String marksText = f.size() == 8 ? f.get(7).strip() : "";

        if (question.isEmpty()) {
            errors.add(at + "the question is empty");
        }

        boolean written = correct.isEmpty() && options.stream().allMatch(String::isEmpty);
        if (!written) {
            for (int i = 0; i < options.size(); i++) {
                if (options.get(i).isEmpty()) {
                    errors.add(at + "option_" + (char) ('a' + i) + " is empty"
                            + " (leave all four options and correct blank for a written question)");
                }
            }
            if (!LABELS.contains(correct)) {
                errors.add(at + "correct must be a, b, c or d, not \"" + f.get(6).strip() + "\"");
            }
        }

        BigDecimal marks = BigDecimal.ONE;
        if (!marksText.isEmpty()) {
            try {
                marks = new BigDecimal(marksText);
                if (marks.signum() <= 0 || marks.compareTo(new BigDecimal("100")) > 0) {
                    errors.add(at + "marks must be above 0 and at most 100");
                }
            } catch (NumberFormatException e) {
                errors.add(at + "marks must be a number, not \"" + marksText + "\"");
            }
        }

        if (errors.size() > before) {
            return null;
        }
        return written
                ? new ParsedQuestion(QuestionType.SHORT_ANSWER, topic.isEmpty() ? null : topic, question,
                        List.of(), null, marks)
                : new ParsedQuestion(QuestionType.MCQ, topic.isEmpty() ? null : topic, question,
                        options, correct, marks);
    }

    private record Row(int line, List<String> fields) {
    }

    static List<Row> tokenize(String text) {
        List<Row> rows = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        int line = 1;
        int rowStart = 1;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    if (c == '\n') {
                        line++;
                    }
                    field.append(c);
                }
            } else if (c == '"' && field.isEmpty()) {
                quoted = true;
            } else if (c == ',') {
                fields.add(field.toString());
                field.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                fields.add(field.toString());
                field.setLength(0);
                rows.add(new Row(rowStart, fields));
                fields = new ArrayList<>();
                line++;
                rowStart = line;
            } else {
                field.append(c);
            }
        }
        if (!field.isEmpty() || !fields.isEmpty()) {
            fields.add(field.toString());
            rows.add(new Row(rowStart, fields));
        }
        return rows;
    }
}
