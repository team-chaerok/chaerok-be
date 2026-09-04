package com.chaerok.backend.global.config;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@TestComponent
@RestController
class SecurityTestController {

    @GetMapping("/api/health")
    String health() {
        return "ok";
    }

    @GetMapping("/api/test/protected")
    String protectedApi() {
        return "ok";
    }

    @GetMapping("/api/admin/test")
    String admin() {
        return "ok";
    }

    @GetMapping("/api/dev/test")
    String dev() {
        return "ok";
    }

    @GetMapping("/actuator/health")
    String actuatorHealth() {
        return "ok";
    }

    @GetMapping("/actuator/prometheus")
    String prometheus() {
        return "ok";
    }

    @GetMapping("/actuator/test")
    String actuatorTest() {
        return "ok";
    }
}