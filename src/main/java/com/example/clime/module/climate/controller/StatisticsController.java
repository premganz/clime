package com.example.clime.module.climate.controller;

import com.example.clime.module.climate.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {
    
    @Autowired
    private StatisticsService statisticsService;
    
    @GetMapping("/rainy-days")
    public ResponseEntity<?> getRainyDaysStatistics(@RequestParam String key) {
        try {
            Map<String, Object> stats = statisticsService.getRainyDaysStatistics(key);
            return ResponseEntity.ok(stats);
        } catch (SecurityException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Invalid unscramble key");
            return ResponseEntity.status(403).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error generating statistics: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    @GetMapping("/rainy-days/chart")
    public ResponseEntity<String> getRainyDaysChart(@RequestParam String key) {
        try {
            String htmlChart = statisticsService.generateRainyDaysChart(key);
            return ResponseEntity.ok(htmlChart);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body("<h3>Error: Invalid unscramble key</h3>");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("<h3>Error: " + e.getMessage() + "</h3>");
        }
    }
    
    @GetMapping("/rainy-days/summer/chart")
    public ResponseEntity<String> getSummerRainyDaysChart(@RequestParam String key) {
        try {
            String chartHtml = statisticsService.generateSeasonalRainyDaysChart(key, "summer");
            return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(chartHtml);
        } catch (SecurityException e) {
            return ResponseEntity.status(401).body("<div class='error'>Invalid key</div>");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("<div class='error'>Error generating summer chart: " + e.getMessage() + "</div>");
        }
    }
    
    @GetMapping("/rainy-days/winter/chart")
    public ResponseEntity<String> getWinterRainyDaysChart(@RequestParam String key) {
        try {
            String chartHtml = statisticsService.generateSeasonalRainyDaysChart(key, "winter");
            return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(chartHtml);
        } catch (SecurityException e) {
            return ResponseEntity.status(401).body("<div class='error'>Invalid key</div>");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("<div class='error'>Error generating winter chart: " + e.getMessage() + "</div>");
        }
    }
}
