package com.example.clime.module.climate.controller;

import com.example.clime.module.climate.service.RainfallAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rainfall")
public class RainfallController {
    
    @Autowired
    private RainfallAnalysisService rainfallAnalysisService;
    
    @GetMapping("/total/chart")
    public ResponseEntity<String> getTotalRainfallChart(@RequestParam String key) {
        try {
            String chartHtml = rainfallAnalysisService.generateTotalRainfallChart(key);
            return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(chartHtml);
        } catch (SecurityException e) {
            return ResponseEntity.status(401).body("<div class='error'>Invalid key</div>");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("<div class='error'>Error generating total rainfall chart: " + e.getMessage() + "</div>");
        }
    }
    
    @GetMapping("/summer/chart")
    public ResponseEntity<String> getSummerRainfallChart(@RequestParam String key) {
        try {
            String chartHtml = rainfallAnalysisService.generateSeasonalRainfallChart(key, "summer");
            return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(chartHtml);
        } catch (SecurityException e) {
            return ResponseEntity.status(401).body("<div class='error'>Invalid key</div>");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("<div class='error'>Error generating summer rainfall chart: " + e.getMessage() + "</div>");
        }
    }
    
    @GetMapping("/winter/chart")
    public ResponseEntity<String> getWinterRainfallChart(@RequestParam String key) {
        try {
            String chartHtml = rainfallAnalysisService.generateSeasonalRainfallChart(key, "winter");
            return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(chartHtml);
        } catch (SecurityException e) {
            return ResponseEntity.status(401).body("<div class='error'>Invalid key</div>");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("<div class='error'>Error generating winter rainfall chart: " + e.getMessage() + "</div>");
        }
    }
}
