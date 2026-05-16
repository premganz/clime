package com.example.clime.module.climatev2.service;

import com.example.clime.module.climatev2.model.WeatherRecord;
import com.opencsv.CSVReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStreamReader;
import java.util.*;

@Service("statisticsServiceV2")
public class StatisticsService {
    
    @Value("${weather.unscramble.key}")
    private String unscrambleKey;
    
    public Map<String, Object> getRainyDaysStatistics(String providedKey) {
        if (!unscrambleKey.equals(providedKey)) {
            throw new SecurityException("Invalid unscramble key");
        }
        
        Map<Integer, Integer> rainyDaysByYear = new LinkedHashMap<>();
        Map<Integer, Integer> totalDaysByYear = new LinkedHashMap<>();
        
        try {
            ClassPathResource resource = new ClassPathResource("scrambled_weather_data.csv");
            try (CSVReader csvReader = new CSVReader(new InputStreamReader(resource.getInputStream()))) {
                
                String[] header = csvReader.readNext(); // Skip header
                String[] line;
                
                while ((line = csvReader.readNext()) != null) {
                    if (line.length >= 20) {
                        try {
                            int year = Integer.parseInt(line[1]);
                            String rainStr = line[11]; // rain column
                            
                            // Count total days for this year
                            totalDaysByYear.put(year, totalDaysByYear.getOrDefault(year, 0) + 1);
                            
                            // Check if it's a rainy day
                            if (isRainyDay(rainStr)) {
                                rainyDaysByYear.put(year, rainyDaysByYear.getOrDefault(year, 0) + 1);
                            }
                        } catch (NumberFormatException e) {
                            // Skip invalid records
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading weather data: " + e.getMessage());
        }
        
        // Calculate percentages and prepare response
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> yearlyData = new ArrayList<>();
        
        for (int year = 2005; year <= 2025; year++) {
            int rainyDays = rainyDaysByYear.getOrDefault(year, 0);
            int totalDays = totalDaysByYear.getOrDefault(year, 0);
            double percentage = totalDays > 0 ? (rainyDays * 100.0 / totalDays) : 0;
            
            if (totalDays > 0) { // Only include years with data
                Map<String, Object> yearData = new HashMap<>();
                yearData.put("year", year);
                yearData.put("rainyDays", rainyDays);
                yearData.put("totalDays", totalDays);
                yearData.put("percentage", Math.round(percentage * 100.0) / 100.0);
                yearlyData.add(yearData);
            }
        }
        
        response.put("yearlyData", yearlyData);
        response.put("totalRainyDays", rainyDaysByYear.values().stream().mapToInt(Integer::intValue).sum());
        response.put("totalDays", totalDaysByYear.values().stream().mapToInt(Integer::intValue).sum());
        
        return response;
    }
    
    public String generateRainyDaysChart(String providedKey) {
        Map<String, Object> stats = getRainyDaysStatistics(providedKey);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> yearlyData = (List<Map<String, Object>>) stats.get("yearlyData");
        
        StringBuilder html = new StringBuilder();
        html.append("<div class='statistics-chart'>");
        html.append("<h3>🌧️ Rainy Days by Year (2005-2025) - ClimateV2</h3>");
        html.append("<div class='chart-container'>");
        
        // Find max value for scaling
        int maxRainyDays = yearlyData.stream()
            .mapToInt(data -> (Integer) data.get("rainyDays"))
            .max()
            .orElse(1);
        
        // Generate bar chart
        html.append("<div class='bar-chart'>");
        for (Map<String, Object> data : yearlyData) {
            int year = (Integer) data.get("year");
            int rainyDays = (Integer) data.get("rainyDays");
            double percentage = (Double) data.get("percentage");
            
            int barHeight = (int) ((rainyDays * 200.0) / maxRainyDays);
            
            html.append("<div class='bar-item'>");
            html.append("<div class='bar' style='height: ").append(barHeight).append("px; background-color: #007bff;'>");
            html.append("<span class='bar-value'>").append(rainyDays).append("</span>");
            html.append("</div>");
            html.append("<div class='bar-label'>").append(year).append("</div>");
            html.append("<div class='bar-percentage'>").append(String.format("%.1f%%", percentage)).append("</div>");
            html.append("</div>");
        }
        html.append("</div>");
        
        // Summary table
        html.append("<div class='summary-table'>");
        html.append("<h4>📊 Summary Statistics</h4>");
        html.append("<table class='table table-striped'>");
        html.append("<thead><tr><th>Year</th><th>Rainy Days</th><th>Total Days</th><th>Percentage</th></tr></thead>");
        html.append("<tbody>");
        
        for (Map<String, Object> data : yearlyData) {
            html.append("<tr>");
            html.append("<td>").append(data.get("year")).append("</td>");
            html.append("<td>").append(data.get("rainyDays")).append("</td>");
            html.append("<td>").append(data.get("totalDays")).append("</td>");
            html.append("<td>").append(String.format("%.1f%%", (Double) data.get("percentage"))).append("</td>");
            html.append("</tr>");
        }
        
        html.append("</tbody>");
        html.append("</table>");
        html.append("</div>");
        html.append("</div>");
        html.append("</div>");
        
        // Add CSS
        html.append("<style>");
        html.append(".statistics-chart { padding: 20px; }");
        html.append(".chart-container { margin: 20px 0; }");
        html.append(".bar-chart { display: flex; align-items: end; justify-content: space-around; height: 250px; margin: 20px 0; border-bottom: 2px solid #333; }");
        html.append(".bar-item { display: flex; flex-direction: column; align-items: center; margin: 0 2px; }");
        html.append(".bar { position: relative; width: 25px; background-color: #007bff; border-radius: 3px 3px 0 0; }");
        html.append(".bar-value { position: absolute; top: -20px; font-size: 10px; font-weight: bold; }");
        html.append(".bar-label { margin-top: 5px; font-size: 12px; font-weight: bold; }");
        html.append(".bar-percentage { font-size: 10px; color: #666; }");
        html.append(".summary-table { margin-top: 20px; }");
        html.append("</style>");
        
        return html.toString();
    }
    
    public String generateSeasonalRainyDaysChart(String providedKey, String season) {
        if (!unscrambleKey.equals(providedKey)) {
            throw new SecurityException("Invalid unscramble key");
        }
        
        Map<Integer, Integer> seasonalRainyDays = new LinkedHashMap<>();
        Map<Integer, Integer> seasonalTotalDays = new LinkedHashMap<>();
        
        // Define season months
        Set<Integer> seasonMonths = getSeasonMonths(season);
        
        try {
            ClassPathResource resource = new ClassPathResource("scrambled_weather_data.csv");
            try (CSVReader csvReader = new CSVReader(new InputStreamReader(resource.getInputStream()))) {
                
                String[] header = csvReader.readNext(); // Skip header
                String[] line;
                
                while ((line = csvReader.readNext()) != null) {
                    if (line.length >= 20) {
                        try {
                            int year = Integer.parseInt(line[1]);
                            int month = Integer.parseInt(line[2]);
                            String rainStr = line[11]; // rain column
                            
                            // Only process if this month is in the requested season
                            if (seasonMonths.contains(month)) {
                                seasonalTotalDays.put(year, seasonalTotalDays.getOrDefault(year, 0) + 1);
                                
                                if (isRainyDay(rainStr)) {
                                    seasonalRainyDays.put(year, seasonalRainyDays.getOrDefault(year, 0) + 1);
                                }
                            }
                        } catch (NumberFormatException e) {
                            // Skip invalid records
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading weather data: " + e.getMessage());
        }
        
        // Generate chart HTML
        StringBuilder html = new StringBuilder();
        html.append("<div class='seasonal-chart'>");
        html.append("<h3>🌧️ ").append(capitalize(season)).append(" Rainy Days by Year - ClimateV2</h3>");
        html.append("<div class='chart-container'>");
        
        // Find max value for scaling
        int maxRainyDays = seasonalRainyDays.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        
        // Generate bar chart
        html.append("<div class='bar-chart'>");
        for (int year = 2005; year <= 2025; year++) {
            int rainyDays = seasonalRainyDays.getOrDefault(year, 0);
            int totalDays = seasonalTotalDays.getOrDefault(year, 0);
            
            if (totalDays > 0) {
                double percentage = (rainyDays * 100.0) / totalDays;
                int barHeight = (int) ((rainyDays * 200.0) / maxRainyDays);
                
                html.append("<div class='bar-item'>");
                html.append("<div class='bar' style='height: ").append(barHeight).append("px; background-color: ").append(getSeasonColor(season)).append(";'>");
                html.append("<span class='bar-value'>").append(rainyDays).append("</span>");
                html.append("</div>");
                html.append("<div class='bar-label'>").append(year).append("</div>");
                html.append("<div class='bar-percentage'>").append(String.format("%.1f%%", percentage)).append("</div>");
                html.append("</div>");
            }
        }
        html.append("</div>");
        html.append("</div>");
        html.append("</div>");
        
        // Add CSS
        html.append("<style>");
        html.append(".seasonal-chart { padding: 20px; }");
        html.append(".chart-container { margin: 20px 0; }");
        html.append(".bar-chart { display: flex; align-items: end; justify-content: space-around; height: 250px; margin: 20px 0; border-bottom: 2px solid #333; }");
        html.append(".bar-item { display: flex; flex-direction: column; align-items: center; margin: 0 2px; }");
        html.append(".bar { position: relative; width: 25px; border-radius: 3px 3px 0 0; }");
        html.append(".bar-value { position: absolute; top: -20px; font-size: 10px; font-weight: bold; }");
        html.append(".bar-label { margin-top: 5px; font-size: 12px; font-weight: bold; }");
        html.append(".bar-percentage { font-size: 10px; color: #666; }");
        html.append("</style>");
        
        return html.toString();
    }
    
    private boolean isRainyDay(String rainStr) {
        if (rainStr == null || rainStr.trim().isEmpty() || "---".equals(rainStr.trim())) {
            return false;
        }
        
        try {
            double rain = Double.parseDouble(rainStr.trim());
            return rain > 0.0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private Set<Integer> getSeasonMonths(String season) {
        switch (season.toLowerCase()) {
            case "spring":
                return Set.of(3, 4, 5);
            case "summer":
                return Set.of(6, 7, 8);
            case "autumn":
            case "fall":
                return Set.of(9, 10, 11);
            case "winter":
                return Set.of(12, 1, 2);
            default:
                return Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        }
    }
    
    private String getSeasonColor(String season) {
        switch (season.toLowerCase()) {
            case "spring":
                return "#28a745"; // Green
            case "summer":
                return "#ffc107"; // Yellow
            case "autumn":
            case "fall":
                return "#fd7e14"; // Orange
            case "winter":
                return "#007bff"; // Blue
            default:
                return "#6c757d"; // Gray
        }
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
