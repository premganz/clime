package com.example.clime.module.climate.controller;

import com.example.clime.module.climate.model.WeatherRecord;
import com.example.clime.module.climate.service.WeatherDataRetrievalService;
import com.example.clime.module.climate.service.WeatherDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

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
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Weather data fetched and processed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error fetching weather data: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
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
                Map<String, Object> response = new HashMap<>();
                response.put("year", year);
                response.put("month", month);
                response.put("recordCount", records.size());
                response.put("data", records);
                return ResponseEntity.ok(response);
            }
        } catch (SecurityException e) {
            System.out.println("❌ [BACKEND] Invalid key provided");
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Invalid unscramble key");
            return ResponseEntity.status(403).body(errorResponse);
        } catch (Exception e) {
            System.out.println("❌ [BACKEND] Error: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error retrieving weather data: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
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
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalRecords", totalRecords);
            response.put("flaggedRecords", flaggedRecords);
            response.put("cleanRecords", totalRecords - flaggedRecords);
            response.put("anomalyPercentage", totalRecords > 0 ? (double) flaggedRecords / totalRecords * 100 : 0);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Invalid unscramble key");
            return ResponseEntity.status(403).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error calculating stats: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
