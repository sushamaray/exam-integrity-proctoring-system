package com.uiu.examintegrity.user;

public enum Role {
    STUDENT,

    FACULTY,

    INVIGILATOR;

    public String authority() {
        return "ROLE_" + name();
    }
}
