package com.example.clime.module.utility.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
@CrossOrigin(origins = "*")
public class HealthController {
    @GetMapping("/api/utility/health")
    public String health() {
        return "Health check from Utility Module!";
    }
}
