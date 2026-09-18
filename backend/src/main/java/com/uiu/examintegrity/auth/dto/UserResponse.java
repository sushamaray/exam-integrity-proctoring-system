package com.uiu.examintegrity.auth.dto;

import com.uiu.examintegrity.user.AppUserDetails;
import com.uiu.examintegrity.user.Role;
import com.uiu.examintegrity.user.User;

public record UserResponse(Long id, String studentId, String fullName, String email, Role role) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getStudentId(), user.getFullName(),
                user.getEmail(), user.getRole());
    }

    public static UserResponse from(AppUserDetails principal) {
        return new UserResponse(principal.getId(), principal.getStudentId(),
                principal.getFullName(), principal.getEmail(), principal.getRole());
    }
}
