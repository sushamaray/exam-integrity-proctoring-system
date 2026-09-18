package com.uiu.examintegrity.exam.domain.csv;

import java.util.List;

public class CsvImportException extends RuntimeException {
    private final List<String> errors;

    public CsvImportException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> errors() {
        return errors;
    }
}
