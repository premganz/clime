package com.example.clime.module.health.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
@CrossOrigin(origins = "*")
public class HealthController {
    @GetMapping("/api/health/hello")
    public String hello() {
        return "Hello from Health Module!";
    }
}
