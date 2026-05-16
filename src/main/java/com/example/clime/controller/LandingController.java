package com.example.clime.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ResponseBody;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@CrossOrigin(origins = "*")
public class LandingController {

    @GetMapping("/")
    public String index() {
        return "index.html";
    }

    @GetMapping("/utility")
    public String utilityPage() {
        return "utility/index.html";
    }

    @GetMapping("/utility2")
    public String utility2Page() {
        return "utility2/index.html";
    }

    @GetMapping("/utility3")
    public String utility3Page() {
        return "utility3/index.html";
    }

    @GetMapping("/climate")
    public String climatePage() {
        return "climate/index.html";
    }

    @GetMapping("/climate-charts")
    public String climateChartsPage() {
        return "climate-charts.html";
    }

    @GetMapping("/api/landing")
    @ResponseBody
    public String landing() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        System.out.println("📡 [BACKEND] Landing endpoint called at " + timestamp);
        return "🚀 BACKEND WORKING! Clime API Response at " + timestamp;
    }

    @GetMapping("/api/health")
    @ResponseBody
    public String healthCheck() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        System.out.println("🏥 [BACKEND] Health endpoint called at " + timestamp);
        return "✅ Health Check OK at " + timestamp;
    }
}
