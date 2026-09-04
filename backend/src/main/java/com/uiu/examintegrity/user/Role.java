package com.uiu.examintegrity.user;

/**
 * Who a user is in an exam. Drives both the API authorities and which dashboard
 * the frontend routes to after login.
 */
public enum Role {

    /** Sits exams, uses practice mode. */
    STUDENT,

    /** Authors questions, owns the question bank, reviews item analysis. */
    FACULTY,

    /** Works the live review queue and makes the final call on flagged sessions. */
    INVIGILATOR;

    /** Spring Security's convention for role authorities. */
    public String authority() {
        return "ROLE_" + name();
    }
}
