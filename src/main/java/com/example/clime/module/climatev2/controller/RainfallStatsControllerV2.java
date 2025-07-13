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
    public ResponseEntity<String> getAllCharts(@RequestParam(required = false) String dataSource) {
        try {
            StringBuilder html = new StringBuilder();
            html.append("<script src='https://cdn.jsdelivr.net/npm/chart.js'></script>");
            html.append("<div class='container-fluid'>");
            
            String sourceLabel = "KWS".equalsIgnoreCase(dataSource) ? "KWS (2000-2025)" : "CSV (1901-2021)";
            
            // Data source info
            html.append("<div class='alert alert-info mb-4'>");
            html.append("<h5>📊 Comprehensive Rainfall Analytics - ").append(sourceLabel).append("</h5>");
            html.append("<p>This page displays all available charts using the ").append(sourceLabel).append(" data source.</p>");
            html.append("</div>");
            
            // Statistics summary
            html.append(rainfallAnalyticsService.generateRainfallStatisticsHtml());
            
            // All charts with data source support
            html.append(rainfallAnalyticsService.generateYearlyRainfallLineChartHtml(dataSource));
            html.append(rainfallAnalyticsService.generateMonthlyAverageChartHtml(dataSource));
            html.append(rainfallAnalyticsService.generateDecadeComparisonChartHtml(dataSource));
            html.append(rainfallAnalyticsService.generateDecadeComparisonChartHtmlWithOffset(0, dataSource));
            
            html.append("</div>");
            return ResponseEntity.ok(html.toString());
        } catch (Exception e) {
            return ResponseEntity.ok("<div class='alert alert-danger'>Error generating charts: " + e.getMessage() + "</div>");
        }
    }
}
