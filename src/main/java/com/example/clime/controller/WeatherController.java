package com.example.clime.controller;

import com.example.clime.model.WeatherRecord;
import com.example.clime.service.WeatherDataRetrievalService;
import com.example.clime.service.WeatherDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {
    
    @Autowired
    private WeatherDataService weatherDataService;
    
    @Autowired
    private WeatherDataRetrievalService weatherDataRetrievalService;
    
    @PostMapping("/fetch-all")
    public ResponseEntity<Map<String, String>> fetchAllWeatherData() {
        try {
            weatherDataService.fetchAndProcessAllData();
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Weather data fetched and processed successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "Error fetching weather data: " + e.getMessage()
            ));
        }
    }
    
    @GetMapping("/data")
    public ResponseEntity<?> getWeatherData(
            @RequestParam String year,
            @RequestParam String month,
            @RequestParam String key,
            @RequestParam(defaultValue = "json") String format) {
        
        System.out.println("🌦️ [BACKEND] Weather data requested: year=" + year + ", month=" + month + ", format=" + format);
        
        try {
            List<WeatherRecord> records = weatherDataRetrievalService.getWeatherData(year, month, key);
            System.out.println("✅ [BACKEND] Retrieved " + records.size() + " weather records");
            
            if ("html".equalsIgnoreCase(format)) {
                String html = weatherDataRetrievalService.generateHtmlTable(records);
                System.out.println("📄 [BACKEND] Generated HTML table (" + html.length() + " characters)");
                return ResponseEntity.ok()
                    .header("Content-Type", "text/html")
                    .body(html);
            } else {
                System.out.println("📊 [BACKEND] Returning JSON data");
                return ResponseEntity.ok(Map.of(
                    "year", year,
                    "month", month,
                    "recordCount", records.size(),
                    "data", records
                ));
            }
        } catch (SecurityException e) {
            System.out.println("❌ [BACKEND] Invalid key provided");
            return ResponseEntity.status(403).body(Map.of(
                "status", "error",
                "message", "Invalid unscramble key"
            ));
        } catch (Exception e) {
            System.out.println("❌ [BACKEND] Error: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "Error retrieving weather data: " + e.getMessage()
            ));
        }
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDataStats(@RequestParam String key) {
        try {
            // Get all data to calculate stats
            int totalRecords = 0;
            int flaggedRecords = 0;
            
            for (int year = 2005; year <= 2025; year++) {
                int startMonth = (year == 2005) ? 9 : 1;
                int endMonth = (year == 2025) ? 6 : 12;
                
                for (int month = startMonth; month <= endMonth; month++) {
                    try {
                        List<WeatherRecord> records = weatherDataRetrievalService.getWeatherData(
                            String.valueOf(year), String.valueOf(month), key);
                        totalRecords += records.size();
                        flaggedRecords += records.stream()
                            .mapToInt(r -> "Y".equals(r.getFlagged()) ? 1 : 0)
                            .sum();
                    } catch (Exception e) {
                        // Skip if no data for this month
                    }
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "totalRecords", totalRecords,
                "flaggedRecords", flaggedRecords,
                "cleanRecords", totalRecords - flaggedRecords,
                "anomalyPercentage", totalRecords > 0 ? (double) flaggedRecords / totalRecords * 100 : 0
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of(
                "status", "error",
                "message", "Invalid unscramble key"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "Error calculating stats: " + e.getMessage()
            ));
        }
    }
}
