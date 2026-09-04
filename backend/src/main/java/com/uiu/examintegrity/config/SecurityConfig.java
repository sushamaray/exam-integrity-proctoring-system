package com.uiu.examintegrity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import com.uiu.examintegrity.user.AppUserDetailsService;

/**
 * Session-cookie authentication for an API consumed by a single-page frontend.
 *
 * <p>Chosen over JWT deliberately: this is a single server that will want
 * server-side session state anyway once live exam sessions exist, and a HttpOnly
 * cookie is not readable by page scripts, which matters for a system whose whole
 * subject is what runs in the exam tab.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Where an authenticated context is stored between requests. Declared as a
     * bean because AuthService needs the same instance to save the context after
     * a manual login.
     */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public AuthenticationManager authenticationManager(AppUserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   SecurityContextRepository securityContextRepository)
            throws Exception {

        return http
                // KNOWN GAP: acceptable while login is the only state-changing
                // endpoint, but CSRF protection has to be switched on before exam
                // submission and proctoring-event endpoints land in update 2.
                .csrf(csrf -> csrf.disable())

                // The SPA renders its own login screen, so the browser must never
                // be handed Spring Security's form or a basic-auth dialog.
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                .securityContext(context -> context.securityContextRepository(securityContextRepository))

                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/api/ping", "/api/auth/login").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        // Everything else is the frontend's own routes and static
                        // assets; the API is the only thing being guarded here.
                        .anyRequest().permitAll())

                // Answer an unauthenticated API call with a bare 401 rather than a
                // redirect to an HTML login page, which fetch() cannot act on.
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpStatus.NO_CONTENT.value())))

                .build();
    }
}
