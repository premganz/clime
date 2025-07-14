package com.example.clime.module.climatev2.controller;

import com.example.clime.module.climatev2.model.RainfallRecord;
import com.example.clime.module.climatev2.service.RainfallDataService;
import com.example.clime.module.climatev2.service.RainfallAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rainfallv2")
public class RainfallControllerV2 {
    // ...existing code...

    @GetMapping("/charts/yearly-svg")
    public ResponseEntity<String> getYearlySvgChart(@RequestParam(defaultValue = "csv") String dataSource) {
        try {
            String html = rainfallAnalyticsService.generateYearlyRainfallLineChartHtml(dataSource);
            return ResponseEntity.ok(html);
        } catch (Exception e) {
            return ResponseEntity.ok("<div class='alert alert-danger'>Error generating yearly SVG chart: " + e.getMessage() + "</div>");
        }
    }

    @GetMapping("/charts/decade-offset")
    public ResponseEntity<String> getDecadeOffsetChart(@RequestParam(defaultValue = "0") int offset) {
        try {
            String html = rainfallAnalyticsService.generateDecadeComparisonChartHtmlWithOffset(offset);
            return ResponseEntity.ok(html);
        } catch (Exception e) {
            return ResponseEntity.ok("<div class='alert alert-danger'>Error generating offset decade chart: " + e.getMessage() + "</div>");
        }
    }

    @Autowired
    @Qualifier("rainfallDataServiceV2")
    private RainfallDataService rainfallDataService;

    @Autowired
    @Qualifier("rainfallAnalyticsServiceV2")
    private RainfallAnalyticsService rainfallAnalyticsService;

    @GetMapping("/data")
    public ResponseEntity<String> getRainfallData(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer startYear,
            @RequestParam(required = false) Integer endYear,
            @RequestParam(defaultValue = "html") String format) {
        
        try {
            List<RainfallRecord> data;
            
            if (year != null) {
                data = rainfallDataService.getDataByYear(year);
                if (data.isEmpty()) {
                    return ResponseEntity.ok("<div class='alert alert-warning'>No rainfall data found for year " + year + "</div>");
                }
            } else if (startYear != null && endYear != null) {
                data = rainfallDataService.getDataByYearRange(startYear, endYear);
            } else {
                // Return last 20 years by default to avoid overwhelming display
                List<RainfallRecord> allData = rainfallDataService.getAllData();
                int totalRecords = allData.size();
                if (totalRecords > 20) {
                    data = allData.subList(totalRecords - 20, totalRecords);
                } else {
                    data = allData;
                }
            }
            
            if ("json".equalsIgnoreCase(format)) {
                // Return simple JSON representation
                StringBuilder json = new StringBuilder();
                json.append("[\n");
                for (int i = 0; i < data.size(); i++) {
                    RainfallRecord record = data.get(i);
                    if (i > 0) json.append(",\n");
                    json.append("  {\n");
                    json.append("    \"year\": ").append(record.getYear()).append(",\n");
                    json.append("    \"total\": ").append(String.format("%.1f", record.getTotal())).append(",\n");
                    json.append("    \"months\": {\n");
                    json.append("      \"jan\": ").append(String.format("%.1f", record.getJan())).append(",\n");
                    json.append("      \"feb\": ").append(String.format("%.1f", record.getFeb())).append(",\n");
                    json.append("      \"mar\": ").append(String.format("%.1f", record.getMar())).append(",\n");
                    json.append("      \"apr\": ").append(String.format("%.1f", record.getApril())).append(",\n");
                    json.append("      \"may\": ").append(String.format("%.1f", record.getMay())).append(",\n");
                    json.append("      \"jun\": ").append(String.format("%.1f", record.getJune())).append(",\n");
                    json.append("      \"jul\": ").append(String.format("%.1f", record.getJuly())).append(",\n");
                    json.append("      \"aug\": ").append(String.format("%.1f", record.getAug())).append(",\n");
                    json.append("      \"sep\": ").append(String.format("%.1f", record.getSept())).append(",\n");
                    json.append("      \"oct\": ").append(String.format("%.1f", record.getOct())).append(",\n");
                    json.append("      \"nov\": ").append(String.format("%.1f", record.getNov())).append(",\n");
                    json.append("      \"dec\": ").append(String.format("%.1f", record.getDec())).append("\n");
                    json.append("    }\n");
                    json.append("  }");
                }
                json.append("\n]");
                return ResponseEntity.ok(json.toString());
            } else {
                String html = rainfallDataService.generateRainfallTableHtml(data);
                return ResponseEntity.ok(html);
            }
            
        } catch (Exception e) {
            return ResponseEntity.ok("<div class='alert alert-danger'>Error retrieving rainfall data: " + e.getMessage() + "</div>");
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<String> getRainfallStats() {
        try {
            String html = rainfallAnalyticsService.generateRainfallStatisticsHtml();
            return ResponseEntity.ok(html);
        } catch (Exception e) {
            return ResponseEntity.ok("<div class='alert alert-danger'>Error generating statistics: " + e.getMessage() + "</div>");
        }
    }

    @GetMapping("/charts/annual")
    public ResponseEntity<String> getAnnualChart() {
        try {
            String html = rainfallAnalyticsService.generateYearlyRainfallLineChartHtml();
            return ResponseEntity.ok(html);
        } catch (Exception e) {
            return ResponseEntity.ok("<div class='alert alert-danger'>Error generating annual chart: " + e.getMessage() + "</div>");
        }
    }

    @GetMapping("/charts/monthly")
    public ResponseEntity<String> getMonthlyChart() {
        try {
            StringBuilder html = new StringBuilder();
            html.append("<script src='https://cdn.jsdelivr.net/npm/chart.js'></script>");
            html.append(rainfallAnalyticsService.generateMonthlyAverageChartHtml());
            return ResponseEntity.ok(html.toString());
        } catch (Exception e) {
            return ResponseEntity.ok("<div class='alert alert-danger'>Error generating monthly chart: " + e.getMessage() + "</div>");
        }
    }

    @GetMapping("/charts/decade")
    public ResponseEntity<String> getDecadeChart() {
        try {
            StringBuilder html = new StringBuilder();
            html.append(rainfallAnalyticsService.generateDecadeComparisonChartHtml());
            return ResponseEntity.ok(html.toString());
        } catch (Exception e) {
            return ResponseEntity.ok("<div class='alert alert-danger'>Error generating decade chart: " + e.getMessage() + "</div>");
        }
    }

    @GetMapping("/charts/monthly-trend")
    public ResponseEntity<String> getMonthlyTrendChart(@RequestParam int month) {
        try {
            String html = rainfallAnalyticsService.generateMonthlyTrendLineChartHtml(month);
            return ResponseEntity.ok(html);
        } catch (Exception e) {
            return ResponseEntity.ok("<div class='alert alert-danger'>Error generating monthly trend chart: " + e.getMessage() + "</div>");
        }
    }
}
