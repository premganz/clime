package com.example.clime.module.climatev2.controller;

import com.example.clime.module.climatev2.service.RainfallAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rainfallstatsv2")
public class RainfallStatsControllerV2 {

    @Autowired
    @Qualifier("rainfallAnalyticsServiceV2")
    private RainfallAnalyticsService rainfallAnalyticsService;

    @GetMapping("/summary")
    public ResponseEntity<String> getRainfallSummary() {
        try {
            String html = rainfallAnalyticsService.generateRainfallStatisticsHtml();
            return ResponseEntity.ok(html);
        } catch (Exception e) {
            return ResponseEntity.ok("<div class='alert alert-danger'>Error generating rainfall summary: " + e.getMessage() + "</div>");
        }
    }

    @GetMapping("/charts/all")
    public ResponseEntity<String> getAllCharts(@RequestParam(defaultValue = "10") int bundleSize) {
        try {
            StringBuilder html = new StringBuilder();
            html.append("<script src='https://cdn.jsdelivr.net/npm/chart.js'></script>");
            html.append("<div class='container-fluid'>");
            
            // Statistics summary
            html.append(rainfallAnalyticsService.generateRainfallStatisticsHtml());
            
            // All charts
            // html.append(rainfallAnalyticsService.generateAnnualRainfallChartHtml());
            html.append(rainfallAnalyticsService.generateYearlyRainfallLineChartHtml());
            html.append(rainfallAnalyticsService.generateMonthlyAverageChartHtml());
            html.append(rainfallAnalyticsService.generateDecadeComparisonChartHtml(bundleSize));
            html.append(rainfallAnalyticsService.generateDecadeComparisonChartHtmlWithOffset(0, bundleSize));
            
            html.append("</div>");
            return ResponseEntity.ok(html.toString());
        } catch (Exception e) {
            return ResponseEntity.ok("<div class='alert alert-danger'>Error generating charts: " + e.getMessage() + "</div>");
        }
    }
}
