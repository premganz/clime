package com.example.clime.module.climatev2.controller;

import com.example.clime.module.climatev2.model.DataSource;
import com.example.clime.module.climatev2.model.RainfallRecord;
import com.example.clime.module.climatev2.service.UnifiedRainfallDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/datasource")
public class DataSourceController {
    
    @Autowired
    private UnifiedRainfallDataService unifiedRainfallDataService;
    
    @PostMapping("/switch")
    public ResponseEntity<Map<String, String>> switchDataSource(@RequestParam String source) {
        Map<String, String> response = new HashMap<>();
        
        try {
            DataSource dataSource = DataSource.valueOf(source.toUpperCase());
            unifiedRainfallDataService.setDataSource(dataSource);
            
            response.put("status", "success");
            response.put("message", "Data source switched to " + dataSource.getDisplayName());
            response.put("currentSource", dataSource.name());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", "Invalid data source: " + source);
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentDataSource() {
        Map<String, Object> response = new HashMap<>();
        DataSource current = unifiedRainfallDataService.getCurrentDataSource();
        
        response.put("currentSource", current.name());
        response.put("displayName", current.getDisplayName());
        response.put("description", current.getDescription());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/info")
    public ResponseEntity<String> getDataSourceInfo() {
        String info = unifiedRainfallDataService.getDataSourceInfo();
        return ResponseEntity.ok(info);
    }
    
    @GetMapping("/data")
    public ResponseEntity<String> getRainfallDataWithCurrentSource(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer startYear,
            @RequestParam(required = false) Integer endYear,
            @RequestParam(defaultValue = "html") String format) {
        
        try {
            List<RainfallRecord> data;
            
            if (year != null) {
                data = unifiedRainfallDataService.getDataByYear(year);
                if (data.isEmpty()) {
                    return ResponseEntity.ok("<div class='alert alert-warning'>No rainfall data found for year " + year + " in current data source</div>");
                }
            } else if (startYear != null && endYear != null) {
                data = unifiedRainfallDataService.getDataByYearRange(startYear, endYear);
            } else {
                // Return last 20 years by default to avoid overwhelming display
                List<RainfallRecord> allData = unifiedRainfallDataService.getAllData();
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
                    json.append("    \"dataSource\": \"").append(unifiedRainfallDataService.getCurrentDataSource().name()).append("\",\n");
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
                String infoHtml = unifiedRainfallDataService.getDataSourceInfo();
                String tableHtml = unifiedRainfallDataService.generateRainfallTableHtml(data);
                return ResponseEntity.ok(infoHtml + tableHtml);
            }
            
        } catch (Exception e) {
            return ResponseEntity.ok("<div class='alert alert-danger'>Error retrieving rainfall data: " + e.getMessage() + "</div>");
        }
    }
}