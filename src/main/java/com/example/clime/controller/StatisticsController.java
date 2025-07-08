package com.example.clime.controller;

import com.example.clime.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
            return ResponseEntity.status(403).body(Map.of(
                "status", "error",
                "message", "Invalid unscramble key"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "Error generating statistics: " + e.getMessage()
            ));
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
}
