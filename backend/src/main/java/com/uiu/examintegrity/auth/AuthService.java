package com.uiu.examintegrity.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uiu.examintegrity.auth.dto.LoginRequest;
import com.uiu.examintegrity.auth.dto.RegisterRequest;
import com.uiu.examintegrity.auth.dto.UserResponse;
import com.uiu.examintegrity.common.ConflictException;
import com.uiu.examintegrity.user.AppUserDetails;
import com.uiu.examintegrity.user.Role;
import com.uiu.examintegrity.user.User;
import com.uiu.examintegrity.user.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager,
                       SecurityContextRepository securityContextRepository,
                       SessionAuthenticationStrategy sessionAuthenticationStrategy,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse login(LoginRequest request, HttpServletRequest httpRequest,
                              HttpServletResponse httpResponse) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));

        return establishSession(authentication, httpRequest, httpResponse);
    }

    @Transactional
    public UserResponse register(RegisterRequest request, HttpServletRequest httpRequest,
                                 HttpServletResponse httpResponse) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("An account with that email already exists");
        }
        if (userRepository.existsByStudentId(request.studentId())) {
            throw new ConflictException("An account with that student ID already exists");
        }

        User user = new User(request.fullName(), request.email(),
                passwordEncoder.encode(request.password()), Role.STUDENT);
        user.setStudentId(request.studentId());
        userRepository.saveAndFlush(user);

        AppUserDetails principal = new AppUserDetails(user);
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());

        return establishSession(authentication, httpRequest, httpResponse);
    }

    private UserResponse establishSession(Authentication authentication,
                                          HttpServletRequest httpRequest,
                                          HttpServletResponse httpResponse) {
        sessionAuthenticationStrategy.onAuthentication(authentication, httpRequest, httpResponse);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        return UserResponse.from((AppUserDetails) authentication.getPrincipal());
    }
}
