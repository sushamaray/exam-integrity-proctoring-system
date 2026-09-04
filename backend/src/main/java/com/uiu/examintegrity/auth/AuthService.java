package com.uiu.examintegrity.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import com.uiu.examintegrity.auth.dto.LoginRequest;
import com.uiu.examintegrity.auth.dto.UserResponse;
import com.uiu.examintegrity.user.AppUserDetails;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Authenticates credentials and establishes the HTTP session.
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthService(AuthenticationManager authenticationManager,
                       SecurityContextRepository securityContextRepository) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    /**
     * Verifies the credentials and stores the resulting security context in the
     * session, which is what makes the JSESSIONID cookie mean anything on the
     * next request.
     *
     * <p>Since Spring Security 6 the context is NOT persisted automatically when
     * you authenticate by hand: without the explicit saveContext call below,
     * login appears to succeed and then /api/auth/me immediately returns 401.
     *
     * @throws org.springframework.security.core.AuthenticationException when the
     *         credentials are wrong or the account is disabled; translated to a
     *         401 response by GlobalExceptionHandler
     */
    public UserResponse login(LoginRequest request, HttpServletRequest httpRequest,
                              HttpServletResponse httpResponse) {

        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        return UserResponse.from((AppUserDetails) authentication.getPrincipal());
    }
}
