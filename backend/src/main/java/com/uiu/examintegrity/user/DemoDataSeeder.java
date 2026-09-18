package com.uiu.examintegrity.user;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@Order(1)
public class DemoDataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String DEMO_PASSWORD = "password123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        String hash = passwordEncoder.encode(DEMO_PASSWORD);
        userRepository.saveAll(List.of(
                new User("Rahim Ahmed", "student@uiu.ac.bd", hash, Role.STUDENT),
                new User("Dr. Farhana Islam", "faculty@uiu.ac.bd", hash, Role.FACULTY),
                new User("Nasir Uddin", "invigilator@uiu.ac.bd", hash, Role.INVIGILATOR)));

        log.info("Seeded {} demo accounts (password: {})", Role.values().length, DEMO_PASSWORD);
    }
}
