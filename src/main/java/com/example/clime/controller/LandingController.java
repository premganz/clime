package com.example.clime.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class LandingController {

    @GetMapping("/landing")
    public String landing() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        System.out.println("📡 [BACKEND] Landing endpoint called at " + timestamp);
        return "🚀 BACKEND WORKING! Clime API Response at " + timestamp;
    }

    @GetMapping("/health")
    public String health() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        System.out.println("🏥 [BACKEND] Health endpoint called at " + timestamp);
        return "✅ Health Check OK at " + timestamp;
    }
}
