package com.uiu.examintegrity.exam.domain;

public record AnswerValue(String optionLabel, String text) {
    public AnswerValue {
        if ((optionLabel == null) == (text == null)) {
            throw new IllegalArgumentException("An answer is either an option or text, not both or neither");
        }
    }

    public static AnswerValue choice(String optionLabel) {
        return new AnswerValue(optionLabel, null);
    }

    public static AnswerValue text(String text) {
        return new AnswerValue(null, text);
    }

    public boolean isChoice() {
        return optionLabel != null;
    }
}
