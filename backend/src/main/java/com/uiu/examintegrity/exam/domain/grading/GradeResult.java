package com.uiu.examintegrity.exam.domain.grading;

import java.math.BigDecimal;

public record GradeResult(boolean pending, BigDecimal marks) {
    public static GradeResult graded(BigDecimal marks) {
        return new GradeResult(false, marks);
    }

    public static GradeResult pendingReview() {
        return new GradeResult(true, null);
    }
}
