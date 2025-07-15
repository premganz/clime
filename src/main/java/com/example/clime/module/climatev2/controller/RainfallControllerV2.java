package com.example.clime.module.climatev2.controller;

import com.example.clime.module.climatev2.model.DataSource;
import com.example.clime.module.climatev2.model.RainfallRecord;
import com.example.clime.module.climatev2.service.RainfallDataService;
import com.example.clime.module.climatev2.service.RainfallAnalyticsService;
import com.example.clime.module.climatev2.service.UnifiedRainfallDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/rainfallv2")
public class RainfallControllerV2 {
    
    @Autowired
    @Qualifier("rainfallDataServiceV2")
    private RainfallDataService rainfallDataService;

    @Autowired
    @Qualifier("rainfallAnalyticsServiceV2")
    private RainfallAnalyticsService rainfallAnalyticsService;
    
    @Autowired
    private UnifiedRainfallDataService unifiedRainfallDataService;

    @GetMapping("/data")
    public ResponseEntity<String> getRainfallData(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer startYear,
            @RequestParam(required = false) Integer endYear,
            @RequestParam(required = false) String excludedYears,
            @RequestParam(defaultValue = "html") String format) {
        
        try {
            List<RainfallRecord> data;
            
            if (year != null) {
                if (excludedYears != null && !excludedYears.trim().isEmpty()) {
                    data = unifiedRainfallDataService.getDataByYear(year, excludedYears);
                } else {
                    data = rainfallDataService.getDataByYear(year);
                }
                if (data.isEmpty()) {
                    return ResponseEntity.ok("<div class='alert alert-warning'>No rainfall data found for year " + year + "</div>");
                }
            } else if (startYear != null && endYear != null) {
                if (excludedYears != null && !excludedYears.trim().isEmpty()) {
                    data = unifiedRainfallDataService.getDataByYearRange(startYear, endYear, excludedYears);
                } else {
                    data = rainfallDataService.getDataByYearRange(startYear, endYear);
                }
            } else {
                // Return last 20 years by default to avoid overwhelming display
                List<RainfallRecord> allData;
                if (excludedYears != null && !excludedYears.trim().isEmpty()) {
                    allData = unifiedRainfallDataService.getAllData(excludedYears);
                } else {
                    allData = rainfallDataService.getAllData();
                }
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
    public ResponseEntity<String> getRainfallStats(@RequestParam(required = false) String excludedYears) {
        try {
            rainfallAnalyticsService.setExcludedYears(excludedYears);
            String html = rainfallAnalyticsService.generateRainfallStatisticsHtml();
            rainfallAnalyticsService.clearExcludedYears();
            return ResponseEntity.ok(html);
        } catch (Exception e) {
            rainfallAnalyticsService.clearExcludedYears();
            return ResponseEntity.ok("<div class='alert alert-danger'>Error generating statistics: " + e.getMessage() + "</div>");
        }
    }

    @GetMapping("/charts/annual")
    public ResponseEntity<String> getAnnualChart(@RequestParam(defaultValue = "CSV") String dataSource,
                                                @RequestParam(required = false) String excludedYears) {
        try {
            // Set data source temporarily and excluded years
            return getChartResponse(dataSource, excludedYears, () -> rainfallAnalyticsService.generateYearlyRainfallLineChartHtml(), "annual chart");
        } catch (Exception e) {
            return ResponseEntity.ok("<div class='alert alert-danger'>Error generating annual chart: " + e.getMessage() + "</div>");
        }
    }

    @GetMapping("/charts/yearly-svg")
    public ResponseEntity<String> getYearlySvgChart(@RequestParam(defaultValue = "CSV") String dataSource) {
        try {
            return getChartResponse(dataSource, () -> rainfallAnalyticsService.generateYearlyRainfallLineChartHtml(), "yearly SVG chart");
        } catch (Exception e) {
            return ResponseEntity.ok("<div class='alert alert-danger'>Error generating yearly SVG chart: " + e.getMessage() + "</div>");
        }
    }

    @GetMapping("/charts/monthly")
    public ResponseEntity<String> getMonthlyChart(@RequestParam(defaultValue = "CSV") String dataSource) {
        try {
            return getChartResponse(dataSource, () -> {
                StringBuilder html = new StringBuilder();
                html.append("<script src='https://cdn.jsdelivr.net/npm/chart.js'></script>");
                html.append(rainfallAnalyticsService.generateMonthlyAverageChartHtml());
                return html.toString();
            }, "monthly chart");
        } catch (Exception e) {
            return ResponseEntity.ok("<div class='alert alert-danger'>Error generating monthly chart: " + e.getMessage() + "</div>");
        }
    }

    @GetMapping("/charts/decade")
    public ResponseEntity<String> getDecadeChart(@RequestParam(defaultValue = "10") int bundleSize, @RequestParam(defaultValue = "CSV") String dataSource) {
        try {
            return getChartResponse(dataSource, () -> {
                StringBuilder html = new StringBuilder();
                html.append(rainfallAnalyticsService.generateBundleComparisonChartHtml(bundleSize));
                return html.toString();
            }, "bundled chart");
        } catch (Exception e) {
            return ResponseEntity.ok("<div class='alert alert-danger'>Error generating bundled chart: " + e.getMessage() + "</div>");
        }
    }

    @GetMapping("/charts/decade-offset")
    public ResponseEntity<String> getDecadeOffsetChart(@RequestParam(defaultValue = "0") int offset, @RequestParam(defaultValue = "10") int bundleSize, @RequestParam(defaultValue = "CSV") String dataSource) {
        try {
            return getChartResponse(dataSource, () -> rainfallAnalyticsService.generateBundleComparisonChartHtmlWithOffset(offset, bundleSize), "bundled chart with offset");
        } catch (Exception e) {
            return ResponseEntity.ok("<div class='alert alert-danger'>Error generating bundled chart with offset: " + e.getMessage() + "</div>");
        }
    }

    @GetMapping("/charts/monthly-trend")
    public ResponseEntity<String> getMonthlyTrendChart(@RequestParam int month, @RequestParam(defaultValue = "CSV") String dataSource) {
        try {
            return getChartResponse(dataSource, () -> rainfallAnalyticsService.generateMonthlyTrendLineChartHtml(month), "monthly trend chart");
        } catch (Exception e) {
            return ResponseEntity.ok("<div class='alert alert-danger'>Error generating monthly trend chart: " + e.getMessage() + "</div>");
        }
    }

    @GetMapping("/charts/year-wise")
    public ResponseEntity<String> getYearWiseChart(@RequestParam(defaultValue = "CSV") String dataSource) {
        try {
            return getChartResponse(dataSource, () -> rainfallAnalyticsService.generateYearWiseRainfallChartHtml(), "year-wise chart");
        } catch (Exception e) {
            return ResponseEntity.ok("<div class='alert alert-danger'>Error generating year-wise chart: " + e.getMessage() + "</div>");
        }
    }

    @GetMapping("/charts/monthly-offset")
    public ResponseEntity<String> getMonthlyOffsetChart(@RequestParam(defaultValue = "1") int monthOffset, @RequestParam(defaultValue = "CSV") String dataSource) {
        try {
            return getChartResponse(dataSource, () -> rainfallAnalyticsService.generateMonthlyOffsetChartHtml(monthOffset), "monthly offset chart");
        } catch (Exception e) {
            return ResponseEntity.ok("<div class='alert alert-danger'>Error generating monthly offset chart: " + e.getMessage() + "</div>");
        }
    }

    @GetMapping("/charts/yearly-monthly-offset")
    public ResponseEntity<String> getYearlyChartWithMonthlyOffset(@RequestParam(defaultValue = "1") int offset, @RequestParam(defaultValue = "CSV") String dataSource) {
        try {
            return getChartResponse(dataSource, () -> rainfallAnalyticsService.generateYearlyBarChartWithMonthlyOffset(offset), "yearly chart with monthly offset");
        } catch (Exception e) {
            return ResponseEntity.ok("<div class='alert alert-danger'>Error generating yearly chart with monthly offset: " + e.getMessage() + "</div>");
        }
    }
    
    private ResponseEntity<String> getChartResponse(String dataSource, Supplier<String> chartGenerator, String chartType) {
        // Set the data source in the unified service
        try {
            DataSource ds = DataSource.valueOf(dataSource.toUpperCase());
            unifiedRainfallDataService.setDataSource(ds);
            
            // Generate chart using the analytics service (now works with both CSV and KWS data)
            String html = chartGenerator.get();
            return ResponseEntity.ok(html);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok("<div class='alert alert-danger'>Invalid data source: " + dataSource + "</div>");
        } catch (Exception e) {
            return ResponseEntity.ok("<div class='alert alert-danger'>Error generating " + chartType + ": " + e.getMessage() + "</div>");
        }
    }
    
    private ResponseEntity<String> getChartResponse(String dataSource, String excludedYears, Supplier<String> chartGenerator, String chartType) {
        // Set the data source in the unified service and excluded years in analytics service
        try {
            DataSource ds = DataSource.valueOf(dataSource.toUpperCase());
            unifiedRainfallDataService.setDataSource(ds);
            
            // Set excluded years for this request
            rainfallAnalyticsService.setExcludedYears(excludedYears);
            
            // Generate chart using the analytics service (now works with both CSV and KWS data)
            String html = chartGenerator.get();
            
            // Clear excluded years after use
            rainfallAnalyticsService.clearExcludedYears();
            
            return ResponseEntity.ok(html);
        } catch (IllegalArgumentException e) {
            rainfallAnalyticsService.clearExcludedYears();
            return ResponseEntity.ok("<div class='alert alert-danger'>Invalid data source: " + dataSource + "</div>");
        } catch (Exception e) {
            rainfallAnalyticsService.clearExcludedYears();
            return ResponseEntity.ok("<div class='alert alert-danger'>Error generating " + chartType + ": " + e.getMessage() + "</div>");
        }
    }
}
