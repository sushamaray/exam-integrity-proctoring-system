package com.uiu.examintegrity.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uiu.examintegrity.auth.dto.LoginRequest;
import com.uiu.examintegrity.auth.dto.UserResponse;
import com.uiu.examintegrity.user.AppUserDetails;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/**
 * Session-based login for the single-page frontend.
 *
 * <p>Logout is deliberately absent here: it is handled by Spring Security's own
 * logout filter, wired to POST /api/auth/logout in SecurityConfig.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request,
                              HttpServletRequest httpRequest,
                              HttpServletResponse httpResponse) {
        return authService.login(request, httpRequest, httpResponse);
    }

    /**
     * Who the current session belongs to. The frontend calls this on page load,
     * which is what stops a browser refresh from logging the user out.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal AppUserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(UserResponse.from(principal));
    }
}
