package com.example.clime.module.climatev2.controller;

import com.example.clime.module.climatev2.model.DataSource;
import com.example.clime.module.climatev2.model.RainfallRecord;
import com.example.clime.module.climatev2.service.RainfallAnalyticsService;
import com.example.clime.module.climatev2.service.UnifiedRainfallDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rainfallstatsv2")
public class RainfallStatsControllerV2 {

    @Autowired
    @Qualifier("rainfallAnalyticsServiceV2")
    private RainfallAnalyticsService rainfallAnalyticsService;
    
    @Autowired
    private UnifiedRainfallDataService unifiedRainfallDataService;

    @GetMapping("/summary")
    public ResponseEntity<String> getRainfallSummary(@RequestParam(defaultValue = "CSV") String dataSource,
                                                     @RequestParam(required = false) String excludedYears) {
        try {
            // Set data source for unified service
            setDataSourceIfValid(dataSource);
            
            // Set excluded years for analytics service
            rainfallAnalyticsService.setExcludedYears(excludedYears);
            
            String html = rainfallAnalyticsService.generateRainfallStatisticsHtml();
            
            // Clear excluded years after use
            rainfallAnalyticsService.clearExcludedYears();
            
            return ResponseEntity.ok(html);
        } catch (Exception e) {
            rainfallAnalyticsService.clearExcludedYears();
            return ResponseEntity.ok("<div class='alert alert-danger'>Error generating rainfall summary: " + e.getMessage() + "</div>");
        }
    }

    @GetMapping("/charts/all")
    public ResponseEntity<String> getAllCharts(@RequestParam(defaultValue = "CSV") String dataSource,
                                              @RequestParam(required = false) String excludedYears) {
        try {
            // Store current data source to restore later
            DataSource originalSource = unifiedRainfallDataService.getCurrentDataSource();
            
            // Set data source for unified service
            setDataSourceIfValid(dataSource);
            
            // Set excluded years for analytics service
            rainfallAnalyticsService.setExcludedYears(excludedYears);
            
            StringBuilder html = new StringBuilder();
            html.append("<script src='https://cdn.jsdelivr.net/npm/chart.js'></script>");
            html.append("<div class='container-fluid'>");
            
            // Add data source info
            html.append(unifiedRainfallDataService.getDataSourceInfo());
            
            // Add excluded years info if applicable
            if (excludedYears != null && !excludedYears.trim().isEmpty()) {
                html.append(unifiedRainfallDataService.getExcludedYearsInfo(excludedYears));
            }
            
            // Check if this is CSV or KWS data and route accordingly
            if (unifiedRainfallDataService.getCurrentDataSource() == DataSource.KWS) {
                // For KWS data, use a simpler approach
                html.append("<div class='alert alert-warning'>");
                html.append("<strong>KWS Data Charts:</strong> Showing basic data table. Advanced analytics charts are optimized for CSV data.");
                html.append("</div>");
                
                // Get KWS data table
                List<RainfallRecord> kwsData;
                if (excludedYears != null && !excludedYears.trim().isEmpty()) {
                    kwsData = unifiedRainfallDataService.getAllData(excludedYears);
                } else {
                    kwsData = unifiedRainfallDataService.getAllData();
                }
                String tableHtml = unifiedRainfallDataService.generateRainfallTableHtml(kwsData);
                html.append(tableHtml);
                
            } else {
                // For CSV data, use full analytics
                html.append(rainfallAnalyticsService.generateRainfallStatisticsHtml());
                html.append(rainfallAnalyticsService.generateYearlyRainfallLineChartHtml());
                html.append(rainfallAnalyticsService.generateMonthlyAverageChartHtml());
                html.append(rainfallAnalyticsService.generateDecadeComparisonChartHtml());
                html.append(rainfallAnalyticsService.generateDecadeComparisonChartHtmlWithOffset(0));
            }
            
            html.append("</div>");
            
            // Clear excluded years after use
            rainfallAnalyticsService.clearExcludedYears();
            
            // Restore original data source
            unifiedRainfallDataService.setDataSource(originalSource);
            
            return ResponseEntity.ok(html.toString());
        } catch (Exception e) {
            rainfallAnalyticsService.clearExcludedYears();
            return ResponseEntity.ok("<div class='alert alert-danger'>Error generating charts: " + e.getMessage() + "</div>");
        }
    }
    
    private void setDataSourceIfValid(String dataSource) {
        try {
            DataSource ds = DataSource.valueOf(dataSource.toUpperCase());
            unifiedRainfallDataService.setDataSource(ds);
        } catch (IllegalArgumentException e) {
            // Invalid data source, use default (CSV)
            unifiedRainfallDataService.setDataSource(DataSource.CSV);
        }
    }
}
