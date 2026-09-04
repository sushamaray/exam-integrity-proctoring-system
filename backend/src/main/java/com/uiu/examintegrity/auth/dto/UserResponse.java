package com.uiu.examintegrity.auth.dto;

import com.uiu.examintegrity.user.AppUserDetails;
import com.uiu.examintegrity.user.Role;
import com.uiu.examintegrity.user.User;

/**
 * The only view of an account that ever crosses the API boundary. Note the
 * absence of the password hash -- that is the point of having this type.
 */
public record UserResponse(Long id, String fullName, String email, Role role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }

    public static UserResponse from(AppUserDetails principal) {
        return new UserResponse(
                principal.getId(), principal.getFullName(), principal.getEmail(), principal.getRole());
    }
}
