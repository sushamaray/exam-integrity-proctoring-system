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
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import com.uiu.examintegrity.user.AppUserDetailsService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new ChangeSessionIdAuthenticationStrategy();
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

                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))

                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                .securityContext(context -> context.securityContextRepository(securityContextRepository))

                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/api/ping", "/api/auth/login", "/api/auth/register")
                                .permitAll()

                        .requestMatchers("/api/faculty/**").hasAnyRole("FACULTY", "INVIGILATOR")
                        .requestMatchers("/api/sessions/*/risk", "/api/exams/*/monitor",
                                "/api/exams/*/similarity", "/api/exams/*/similarity/**",
                                "/api/similarity/**",
                                // Carries the spread of answers across every option, which is close
                                // enough to an answer key that a student must not be able to read it.
                                "/api/exams/*/item-analysis", "/api/exams/*/item-analysis/**")
                                .hasAnyRole("FACULTY", "INVIGILATOR")

                        // Students need the socket too, for the output of a program they are
                        // running. Opening the handshake does NOT open the data: every
                        // subscription is authorized per destination in WebSocketConfig, which
                        // refuses any topic it does not recognise, keeps the risk topics staff
                        // only, and lets a student read a console topic only if the run is theirs.
                        .requestMatchers("/ws/**").authenticated()
                        .requestMatchers("/api/student/**").hasRole("STUDENT")
                        .requestMatchers("/api/exams/*/sessions", "/api/sessions/**").hasRole("STUDENT")

                        .requestMatchers("/api/**").authenticated()

                        .anyRequest().permitAll())

                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)

                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpStatus.NO_CONTENT.value())))

                .build();
    }
}
