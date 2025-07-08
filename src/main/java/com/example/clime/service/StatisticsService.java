package com.example.clime.service;

import com.example.clime.model.WeatherRecord;
import com.opencsv.CSVReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStreamReader;
import java.util.*;

@Service
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
        html.append("<h3>🌧️ Rainy Days by Year (2005-2025)</h3>");
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
            int totalDays = (Integer) data.get("totalDays");
            double percentage = (Double) data.get("percentage");
            
            // Calculate bar height (max 200px)
            int barHeight = (int) ((rainyDays * 200.0) / maxRainyDays);
            
            html.append("<div class='bar-item'>");
            html.append("<div class='bar' style='height: ").append(barHeight).append("px; background: linear-gradient(to top, #3498db, #2980b9);'>");
            html.append("<span class='bar-value'>").append(rainyDays).append("</span>");
            html.append("</div>");
            html.append("<div class='bar-label'>");
            html.append("<div class='year'>").append(year).append("</div>");
            html.append("<div class='percentage'>").append(percentage).append("%</div>");
            html.append("<div class='total'>(").append(totalDays).append(" days)</div>");
            html.append("</div>");
            html.append("</div>");
        }
        html.append("</div>");
        
        // Summary statistics
        html.append("<div class='summary-stats'>");
        html.append("<h4>Summary</h4>");
        html.append("<p><strong>Total Rainy Days:</strong> ").append(stats.get("totalRainyDays")).append("</p>");
        html.append("<p><strong>Total Days Analyzed:</strong> ").append(stats.get("totalDays")).append("</p>");
        html.append("<p><strong>Overall Rainy Day Percentage:</strong> ");
        
        int totalRainy = (Integer) stats.get("totalRainyDays");
        int totalDays = (Integer) stats.get("totalDays");
        double overallPercentage = totalDays > 0 ? (totalRainy * 100.0 / totalDays) : 0;
        html.append(Math.round(overallPercentage * 100.0) / 100.0).append("%</p>");
        html.append("</div>");
        
        html.append("</div>");
        html.append("</div>");
        
        // Add CSS styles
        html.append("<style>");
        html.append(".statistics-chart { margin: 20px 0; }");
        html.append(".chart-container { background: #f8f9fa; padding: 20px; border-radius: 8px; }");
        html.append(".bar-chart { display: flex; align-items: end; gap: 8px; margin: 20px 0; min-height: 250px; }");
        html.append(".bar-item { display: flex; flex-direction: column; align-items: center; min-width: 50px; }");
        html.append(".bar { position: relative; border-radius: 4px 4px 0 0; min-height: 10px; width: 40px; transition: all 0.3s; }");
        html.append(".bar:hover { opacity: 0.8; transform: scale(1.05); }");
        html.append(".bar-value { position: absolute; top: -20px; color: #2c3e50; font-weight: bold; font-size: 12px; width: 100%; text-align: center; }");
        html.append(".bar-label { margin-top: 8px; text-align: center; font-size: 11px; }");
        html.append(".year { font-weight: bold; color: #2c3e50; }");
        html.append(".percentage { color: #3498db; font-weight: bold; }");
        html.append(".total { color: #7f8c8d; }");
        html.append(".summary-stats { background: #ecf0f1; padding: 15px; border-radius: 6px; margin-top: 20px; }");
        html.append(".summary-stats h4 { margin-top: 0; color: #2c3e50; }");
        html.append(".summary-stats p { margin: 5px 0; }");
        html.append("</style>");
        
        return html.toString();
    }
    
    public String generateSeasonalRainyDaysChart(String providedKey, String season) {
        Map<String, Object> stats = getSeasonalRainyDaysStatistics(providedKey, season);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> yearlyData = (List<Map<String, Object>>) stats.get("yearlyData");
        
        String seasonTitle = "summer".equals(season) ? "Summer (Mar-Aug)" : "Winter (Sep-Feb)";
        String seasonEmoji = "summer".equals(season) ? "☀️" : "❄️";
        
        StringBuilder html = new StringBuilder();
        html.append("<div class='statistics-chart'>");
        html.append("<h3>").append(seasonEmoji).append(" ").append(seasonTitle).append(" Rainy Days by Year (2005-2025)</h3>");
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
            int totalDays = (Integer) data.get("totalDays");
            double percentage = (Double) data.get("percentage");
            
            // Calculate bar height (max 200px)
            int barHeight = (int) ((rainyDays * 200.0) / maxRainyDays);
            
            // Different color schemes for seasons
            String barColor = "summer".equals(season) ? 
                "linear-gradient(to top, #f39c12, #e67e22)" : 
                "linear-gradient(to top, #3498db, #2980b9)";
            
            html.append("<div class='bar-item'>");
            html.append("<div class='bar' style='height: ").append(barHeight).append("px; background: ").append(barColor).append(";'>");
            html.append("<span class='bar-value'>").append(rainyDays).append("</span>");
            html.append("</div>");
            html.append("<div class='bar-label'>");
            html.append("<div class='year'>").append(year).append("</div>");
            html.append("<div class='percentage'>").append(String.format("%.2f", percentage)).append("%</div>");
            html.append("<div class='total'>(").append(totalDays).append(" days)</div>");
            html.append("</div>");
            html.append("</div>");
        }
        html.append("</div>");
        
        // Add summary statistics
        html.append("<div class='summary-stats'>");
        html.append("<h4>").append(seasonTitle).append(" Summary</h4>");
        html.append("<p><strong>Total Rainy Days:</strong> ").append(stats.get("totalRainyDays")).append("</p>");
        html.append("<p><strong>Total Days Analyzed:</strong> ").append(stats.get("totalDays")).append("</p>");
        html.append("<p><strong>Overall Rainy Day Percentage:</strong> ").append(stats.get("overallPercentage")).append("%</p>");
        html.append("</div>");
        
        html.append("</div>"); // Close chart-container
        html.append("</div>"); // Close statistics-chart
        
        // Add CSS styles
        html.append("<style>");
        html.append(".statistics-chart { margin: 20px 0; }");
        html.append(".chart-container { background: #f8f9fa; padding: 20px; border-radius: 8px; }");
        html.append(".bar-chart { display: flex; align-items: end; gap: 8px; margin: 20px 0; min-height: 250px; }");
        html.append(".bar-item { display: flex; flex-direction: column; align-items: center; min-width: 50px; }");
        html.append(".bar { position: relative; border-radius: 4px 4px 0 0; min-height: 10px; width: 40px; transition: all 0.3s; }");
        html.append(".bar:hover { opacity: 0.8; transform: scale(1.05); }");
        html.append(".bar-value { position: absolute; top: -20px; color: #2c3e50; font-weight: bold; font-size: 12px; width: 100%; text-align: center; }");
        html.append(".bar-label { margin-top: 8px; text-align: center; font-size: 11px; }");
        html.append(".year { font-weight: bold; color: #2c3e50; }");
        html.append(".percentage { color: #3498db; font-weight: bold; }");
        html.append(".total { color: #7f8c8d; }");
        html.append(".summary-stats { background: #ecf0f1; padding: 15px; border-radius: 6px; margin-top: 20px; }");
        html.append(".summary-stats h4 { margin-top: 0; color: #2c3e50; }");
        html.append(".summary-stats p { margin: 5px 0; }");
        html.append("</style>");
        
        return html.toString();
    }
    
    public Map<String, Object> getSeasonalRainyDaysStatistics(String providedKey, String season) {
        if (!unscrambleKey.equals(providedKey)) {
            throw new SecurityException("Invalid unscramble key");
        }
        
        System.out.println("DEBUG: Processing seasonal data for season: " + season);
        
        List<WeatherRecord> allRecords = new ArrayList<>();
        Map<Integer, Integer> yearlyRainyDays = new HashMap<>();
        Map<Integer, Integer> yearlyTotalDays = new HashMap<>();
        
        int processedRecords = 0;
        int matchedRecords = 0;
        
        try (CSVReader reader = new CSVReader(new InputStreamReader(
                new ClassPathResource("scrambled_weather_data.csv").getInputStream()))) {
            
            // Skip header
            reader.readNext();
            
            String[] nextLine;
            Set<Integer> uniqueMonths = new HashSet<>();
            while ((nextLine = reader.readNext()) != null) {
                processedRecords++;
                if (nextLine.length >= 12) {
                    try {
                        int year = Integer.parseInt(nextLine[1]);  // Changed from 0 to 1
                        int month = Integer.parseInt(nextLine[2]); // Changed from 1 to 2
                        String rainStr = nextLine[11];             // Changed from 3 to 11 (rain column)
                        
                        uniqueMonths.add(month);
                        
                        int seasonYear = getSeasonYear(year, month, season);
                        if (seasonYear != -1) {
                            matchedRecords++;
                            yearlyTotalDays.put(seasonYear, yearlyTotalDays.getOrDefault(seasonYear, 0) + 1);
                            if (isRainyDay(rainStr)) {
                                yearlyRainyDays.put(seasonYear, yearlyRainyDays.getOrDefault(seasonYear, 0) + 1);
                            }
                        }
                    } catch (NumberFormatException e) {
                        // Skip invalid rows
                    }
                }
            }
            
            System.out.println("DEBUG: Months found in data: " + uniqueMonths);
        } catch (Exception e) {
            throw new RuntimeException("Error reading weather data", e);
        }
        
        System.out.println("DEBUG: Processed " + processedRecords + " records, matched " + matchedRecords + " for season " + season);
        System.out.println("DEBUG: Yearly total days: " + yearlyTotalDays);
        System.out.println("DEBUG: Yearly rainy days: " + yearlyRainyDays);
        
        // Create yearly data list
        List<Map<String, Object>> yearlyData = new ArrayList<>();
        for (int year = 2005; year <= 2025; year++) {
            int rainyDays = yearlyRainyDays.getOrDefault(year, 0);
            int totalDays = yearlyTotalDays.getOrDefault(year, 0);
            
            if (totalDays > 0) {
                Map<String, Object> yearData = new HashMap<>();
                yearData.put("year", year);
                yearData.put("rainyDays", rainyDays);
                yearData.put("totalDays", totalDays);
                yearData.put("percentage", Math.round((rainyDays * 100.0 / totalDays) * 100.0) / 100.0);
                yearlyData.add(yearData);
            }
        }
        
        // Calculate totals
        int totalRainyDays = yearlyRainyDays.values().stream().mapToInt(Integer::intValue).sum();
        int totalDays = yearlyTotalDays.values().stream().mapToInt(Integer::intValue).sum();
        double overallPercentage = totalDays > 0 ? (totalRainyDays * 100.0 / totalDays) : 0;
        
        Map<String, Object> response = new HashMap<>();
        response.put("yearlyData", yearlyData);
        response.put("totalRainyDays", totalRainyDays);
        response.put("totalDays", totalDays);
        response.put("overallPercentage", Math.round(overallPercentage * 100.0) / 100.0);
        
        return response;
    }
    
    private int getSeasonYear(int year, int month, String season) {
        // Debug logging
        if (year == 2020 && (month == 3 || month == 7)) {
            System.out.println("DEBUG getSeasonYear: year=" + year + ", month=" + month + ", season=" + season);
        }
        
        if ("summer".equals(season)) {
            // Summer: Mar-Aug (months 3-8)
            if (month >= 3 && month <= 8) {
                if (year == 2020 && month == 7) {
                    System.out.println("DEBUG: Returning year " + year + " for summer month " + month);
                }
                return year;
            }
        } else if ("winter".equals(season)) {
            // Winter: Sep-Feb (months 9-12 and 1-2)
            if (month >= 9) {
                return year; // Sep-Dec of year Y represents winter Y
            } else if (month <= 2) {
                return year - 1; // Jan-Feb of year Y+1 represents winter Y
            }
        }
        return -1; // Not in the requested season
    }
    
    private boolean isRainyDay(String rainStr) {
        // Handle null, empty, or common "no rain" indicators
        if (rainStr == null || rainStr.trim().isEmpty()) {
            return false;
        }
        
        String cleanRain = rainStr.trim();
        
        // Handle common "no rain" indicators
        if ("0".equals(cleanRain) || "0.0".equals(cleanRain) || 
            "---".equals(cleanRain) || "N/A".equalsIgnoreCase(cleanRain) ||
            "-".equals(cleanRain) || "".equals(cleanRain)) {
            return false;
        }
        
        try {
            // Try to parse the rain value directly
            double rainValue = Double.parseDouble(cleanRain);
            return rainValue > 0.0;
        } catch (NumberFormatException e) {
            // If direct parsing fails, try to extract numeric part
            try {
                String numericStr = cleanRain.replaceAll("[^0-9.]", "");
                if (!numericStr.isEmpty() && !".".equals(numericStr)) {
                    double rainValue = Double.parseDouble(numericStr);
                    return rainValue > 0.0;
                }
            } catch (NumberFormatException ex) {
                // If we still can't parse it, assume no rain
                return false;
            }
        }
        
        return false;
    }
}
