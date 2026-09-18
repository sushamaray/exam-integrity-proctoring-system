package com.uiu.examintegrity.similarity.domain;

import java.util.Map;
import java.util.Set;

import com.uiu.examintegrity.question.QuestionType;

public record SessionAnswers(Long sessionId, String student, Map<Long, Answer> answers) {
    public SessionAnswers {
        answers = Map.copyOf(answers);
    }

    public record Answer(Long questionId, QuestionType type, String optionLabel, Boolean correct,
            String text, String normalized) {
        public Answer withNormalized(String value) {
            return new Answer(questionId, type, optionLabel, correct, text, value);
        }
    }

    public Set<Long> questionIds() {
        return answers.keySet();
    }

    public Answer answerTo(Long questionId) {
        return answers.get(questionId);
    }

    public boolean isWrong(Long questionId) {
        Answer answer = answers.get(questionId);
        return answer != null && answer.optionLabel() != null && Boolean.FALSE.equals(answer.correct());
    }

    public String optionFor(Long questionId) {
        Answer answer = answers.get(questionId);
        return answer == null ? null : answer.optionLabel();
    }
}
