package com.uiu.examintegrity.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unauthenticated liveness check. Useful for confirming the frontend dev proxy
 * reaches the backend before anything else is wired up.
 */
@RestController
public class PingController {

    @GetMapping("/api/ping")
    public String ping() {
        return "backend alive";
    }
}
