package com.example.clime.service;

import com.opencsv.CSVReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStreamReader;
import java.util.*;

@Service
public class RainfallAnalysisService {
    
    @Value("${weather.unscramble.key}")
    private String unscrambleKey;
    
    public String generateTotalRainfallChart(String providedKey) {
        Map<String, Object> stats = getTotalRainfallStatistics(providedKey);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> yearlyData = (List<Map<String, Object>>) stats.get("yearlyData");
        
        StringBuilder html = new StringBuilder();
        html.append("<div class='statistics-chart'>");
        html.append("<h3>🌧️ Total Rainfall by Year (2005-2025) - mm</h3>");
        html.append("<div class='chart-container'>");
        
        // Find max value for scaling
        double maxRainfall = yearlyData.stream()
            .mapToDouble(data -> (Double) data.get("totalRainfall"))
            .max()
            .orElse(1.0);
        
        // Generate bar chart
        html.append("<div class='bar-chart'>");
        for (Map<String, Object> data : yearlyData) {
            int year = (Integer) data.get("year");
            double totalRainfall = (Double) data.get("totalRainfall");
            int totalDays = (Integer) data.get("totalDays");
            double avgRainfall = (Double) data.get("avgRainfall");
            
            // Calculate bar height (max 250px)
            int barHeight = (int) ((totalRainfall * 250.0) / maxRainfall);
            
            html.append("<div class='bar-item'>");
            html.append("<div class='bar' style='height: ").append(barHeight).append("px; background: linear-gradient(to top, #3498db, #2980b9);'>");
            html.append("<span class='bar-value'>").append(String.format("%.1f", totalRainfall)).append("mm</span>");
            html.append("</div>");
            html.append("<div class='bar-label'>");
            html.append("<div class='year'>").append(year).append("</div>");
            html.append("<div class='total'>").append(String.format("%.1f", totalRainfall)).append("mm</div>");
            html.append("<div class='average'>Avg: ").append(String.format("%.2f", avgRainfall)).append("mm/day</div>");
            html.append("<div class='days'>(").append(totalDays).append(" days)</div>");
            html.append("</div>");
            html.append("</div>");
        }
        html.append("</div>");
        
        // Add summary statistics
        // Add CSS
        html.append("<style>");
        html.append(".statistics-chart { margin: 20px 0; }");
        html.append(".chart-container { background: #f8f9fa; padding: 15px; border-radius: 8px; overflow-x: auto; }");
        html.append(".bar-chart { display: flex; align-items: end; gap: 4px; margin: 15px 0; min-height: 280px; max-width: 100%; }");
        html.append(".bar-item { display: flex; flex-direction: column; align-items: center; min-width: 35px; flex: 1; max-width: 45px; }");
        html.append(".bar { position: relative; border-radius: 3px 3px 0 0; min-height: 8px; width: 100%; max-width: 40px; transition: all 0.3s; margin: 0 auto; }");
        html.append(".bar:hover { opacity: 0.8; transform: scale(1.1); }");
        html.append(".bar-value { position: absolute; top: -22px; color: #2c3e50; font-weight: bold; font-size: 7px; width: 100%; text-align: center; }");
        html.append(".bar-label { margin-top: 6px; text-align: center; font-size: 7px; line-height: 1.1; }");
        html.append(".year { font-weight: bold; color: #2c3e50; margin-bottom: 1px; font-size: 8px; }");
        html.append(".total { color: #3498db; font-weight: bold; font-size: 7px; }");
        html.append(".average { color: #27ae60; font-size: 7px; }");
        html.append(".days { color: #7f8c8d; font-size: 7px; }");
        html.append(".summary-stats { background: #ecf0f1; padding: 12px; border-radius: 6px; margin-top: 15px; }");
        html.append(".summary-stats h4 { margin-top: 0; color: #2c3e50; font-size: 16px; }");
        html.append(".summary-stats p { margin: 4px 0; font-size: 14px; }");
        html.append("</style>");
        
        return html.toString();
    }
    
    public String generateSeasonalRainfallChart(String providedKey, String season) {
        Map<String, Object> stats = getSeasonalRainfallStatistics(providedKey, season);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> yearlyData = (List<Map<String, Object>>) stats.get("yearlyData");
        
        String seasonTitle = "summer".equals(season) ? "Summer (Mar-Aug)" : "Winter (Sep-Feb)";
        String seasonEmoji = "summer".equals(season) ? "☀️" : "❄️";
        
        StringBuilder html = new StringBuilder();
        html.append("<div class='statistics-chart'>");
        html.append("<h3>").append(seasonEmoji).append(" ").append(seasonTitle).append(" Rainfall by Year (2005-2025) - mm</h3>");
        html.append("<div class='chart-container'>");
        
        // Find max value for scaling
        double maxRainfall = yearlyData.stream()
            .mapToDouble(data -> (Double) data.get("totalRainfall"))
            .max()
            .orElse(1.0);
        
        // Generate bar chart
        html.append("<div class='bar-chart'>");
        for (Map<String, Object> data : yearlyData) {
            int year = (Integer) data.get("year");
            double totalRainfall = (Double) data.get("totalRainfall");
            int totalDays = (Integer) data.get("totalDays");
            double avgRainfall = (Double) data.get("avgRainfall");
            
            // Calculate bar height (max 250px)
            int barHeight = (int) ((totalRainfall * 250.0) / maxRainfall);
            
            // Different color schemes for seasons
            String barColor = "summer".equals(season) ? 
                "linear-gradient(to top, #f39c12, #e67e22)" : 
                "linear-gradient(to top, #3498db, #2980b9)";
            
            html.append("<div class='bar-item'>");
            html.append("<div class='bar' style='height: ").append(barHeight).append("px; background: ").append(barColor).append(";'>");
            html.append("<span class='bar-value'>").append(String.format("%.1f", totalRainfall)).append("mm</span>");
            html.append("</div>");
            html.append("<div class='bar-label'>");
            html.append("<div class='year'>").append(year).append("</div>");
            html.append("<div class='total'>").append(String.format("%.1f", totalRainfall)).append("mm</div>");
            html.append("<div class='average'>Avg: ").append(String.format("%.2f", avgRainfall)).append("mm/day</div>");
            html.append("<div class='days'>(").append(totalDays).append(" days)</div>");
            html.append("</div>");
            html.append("</div>");
        }
        html.append("</div>");
        
        html.append("<div class='summary-stats'>");
        html.append("<h4>").append(seasonTitle).append(" Summary</h4>");
        html.append("<p><strong>Total Rainfall:</strong> ").append(String.format("%.1f", (Double) stats.get("totalRainfall"))).append(" mm</p>");
        html.append("<p><strong>Total Days Analyzed:</strong> ").append(stats.get("totalDays")).append("</p>");
        html.append("<p><strong>Average Daily Rainfall:</strong> ").append(String.format("%.2f", (Double) stats.get("avgDailyRainfall"))).append(" mm/day</p>");
        html.append("</div>");
        
        html.append("</div>"); // Close chart-container
        html.append("</div>"); // Close statistics-chart
        
        // Add CSS
        html.append("<style>");
        html.append(".statistics-chart { margin: 20px 0; }");
        html.append(".chart-container { background: #f8f9fa; padding: 15px; border-radius: 8px; overflow-x: auto; }");
        html.append(".bar-chart { display: flex; align-items: end; gap: 5px; margin: 15px 0; min-height: 280px; max-width: 100%; }");
        html.append(".bar-item { display: flex; flex-direction: column; align-items: center; min-width: 45px; flex: 1; max-width: 50px; }");
        html.append(".bar { position: relative; border-radius: 3px 3px 0 0; min-height: 8px; width: 100%; max-width: 40px; transition: all 0.3s; margin: 0 auto; }");
        html.append(".bar:hover { opacity: 0.8; transform: scale(1.1); }");
        html.append(".bar-value { position: absolute; top: -22px; color: #2c3e50; font-weight: bold; font-size: 7px; width: 100%; text-align: center; }");
        html.append(".bar-label { margin-top: 6px; text-align: center; font-size: 7px; line-height: 1.1; }");
        html.append(".year { font-weight: bold; color: #2c3e50; margin-bottom: 1px; font-size: 8px; }");
        html.append(".total { color: #3498db; font-weight: bold; font-size: 7px; }");
        html.append(".average { color: #27ae60; font-size: 7px; }");
        html.append(".days { color: #7f8c8d; font-size: 7px; }");
        html.append(".summary-stats { background: #ecf0f1; padding: 12px; border-radius: 6px; margin-top: 15px; }");
        html.append(".summary-stats h4 { margin-top: 0; color: #2c3e50; font-size: 16px; }");
        html.append(".summary-stats p { margin: 4px 0; font-size: 14px; }");
        html.append("</style>");
        
        return html.toString();
    }
    
    public Map<String, Object> getTotalRainfallStatistics(String providedKey) {
        if (!unscrambleKey.equals(providedKey)) {
            throw new SecurityException("Invalid unscramble key");
        }
        
        Map<Integer, Double> yearlyRainfall = new HashMap<>();
        Map<Integer, Integer> yearlyDays = new HashMap<>();
        
        try (CSVReader reader = new CSVReader(new InputStreamReader(
                new ClassPathResource("scrambled_weather_data.csv").getInputStream()))) {
            
            // Skip header
            reader.readNext();
            
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                if (nextLine.length >= 12) {
                    try {
                        int year = Integer.parseInt(nextLine[1]);
                        String rainStr = nextLine[11]; // Rain column
                        
                        double rainValue = parseRainValue(rainStr);
                        yearlyRainfall.put(year, yearlyRainfall.getOrDefault(year, 0.0) + rainValue);
                        yearlyDays.put(year, yearlyDays.getOrDefault(year, 0) + 1);
                        
                    } catch (NumberFormatException e) {
                        // Skip invalid rows
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading weather data", e);
        }
        
        // Create yearly data list
        List<Map<String, Object>> yearlyData = new ArrayList<>();
        for (int year = 2005; year <= 2025; year++) {
            if (yearlyDays.containsKey(year)) {
                double totalRainfall = yearlyRainfall.getOrDefault(year, 0.0);
                int totalDays = yearlyDays.get(year);
                double avgRainfall = totalDays > 0 ? totalRainfall / totalDays : 0.0;
                
                Map<String, Object> yearData = new HashMap<>();
                yearData.put("year", year);
                yearData.put("totalRainfall", Math.round(totalRainfall * 10.0) / 10.0);
                yearData.put("totalDays", totalDays);
                yearData.put("avgRainfall", Math.round(avgRainfall * 100.0) / 100.0);
                yearlyData.add(yearData);
            }
        }
        
        // Calculate totals
        double totalRainfall = yearlyRainfall.values().stream().mapToDouble(Double::doubleValue).sum();
        int totalDays = yearlyDays.values().stream().mapToInt(Integer::intValue).sum();
        double avgDailyRainfall = totalDays > 0 ? totalRainfall / totalDays : 0.0;
        
        Map<String, Object> response = new HashMap<>();
        response.put("yearlyData", yearlyData);
        response.put("totalRainfall", Math.round(totalRainfall * 10.0) / 10.0);
        response.put("totalDays", totalDays);
        response.put("avgDailyRainfall", Math.round(avgDailyRainfall * 100.0) / 100.0);
        
        return response;
    }
    
    public Map<String, Object> getSeasonalRainfallStatistics(String providedKey, String season) {
        if (!unscrambleKey.equals(providedKey)) {
            throw new SecurityException("Invalid unscramble key");
        }
        
        Map<Integer, Double> yearlyRainfall = new HashMap<>();
        Map<Integer, Integer> yearlyDays = new HashMap<>();
        
        try (CSVReader reader = new CSVReader(new InputStreamReader(
                new ClassPathResource("scrambled_weather_data.csv").getInputStream()))) {
            
            // Skip header
            reader.readNext();
            
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                if (nextLine.length >= 12) {
                    try {
                        int year = Integer.parseInt(nextLine[1]);
                        int month = Integer.parseInt(nextLine[2]);
                        String rainStr = nextLine[11]; // Rain column
                        
                        int seasonYear = getSeasonYear(year, month, season);
                        if (seasonYear != -1) {
                            double rainValue = parseRainValue(rainStr);
                            yearlyRainfall.put(seasonYear, yearlyRainfall.getOrDefault(seasonYear, 0.0) + rainValue);
                            yearlyDays.put(seasonYear, yearlyDays.getOrDefault(seasonYear, 0) + 1);
                        }
                    } catch (NumberFormatException e) {
                        // Skip invalid rows
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading weather data", e);
        }
        
        // Create yearly data list
        List<Map<String, Object>> yearlyData = new ArrayList<>();
        for (int year = 2005; year <= 2025; year++) {
            if (yearlyDays.containsKey(year)) {
                double totalRainfall = yearlyRainfall.getOrDefault(year, 0.0);
                int totalDays = yearlyDays.get(year);
                double avgRainfall = totalDays > 0 ? totalRainfall / totalDays : 0.0;
                
                Map<String, Object> yearData = new HashMap<>();
                yearData.put("year", year);
                yearData.put("totalRainfall", Math.round(totalRainfall * 10.0) / 10.0);
                yearData.put("totalDays", totalDays);
                yearData.put("avgRainfall", Math.round(avgRainfall * 100.0) / 100.0);
                yearlyData.add(yearData);
            }
        }
        
        // Calculate totals
        double totalRainfall = yearlyRainfall.values().stream().mapToDouble(Double::doubleValue).sum();
        int totalDays = yearlyDays.values().stream().mapToInt(Integer::intValue).sum();
        double avgDailyRainfall = totalDays > 0 ? totalRainfall / totalDays : 0.0;
        
        Map<String, Object> response = new HashMap<>();
        response.put("yearlyData", yearlyData);
        response.put("totalRainfall", Math.round(totalRainfall * 10.0) / 10.0);
        response.put("totalDays", totalDays);
        response.put("avgDailyRainfall", Math.round(avgDailyRainfall * 100.0) / 100.0);
        
        return response;
    }
    
    private int getSeasonYear(int year, int month, String season) {
        if ("summer".equals(season)) {
            // Summer: Mar-Aug (months 3-8)
            if (month >= 3 && month <= 8) {
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
    
    private double parseRainValue(String rainStr) {
        // Handle null, empty, or common "no rain" indicators
        if (rainStr == null || rainStr.trim().isEmpty()) {
            return 0.0;
        }
        
        String cleanRain = rainStr.trim();
        
        // Handle common "no rain" indicators
        if ("0".equals(cleanRain) || "0.0".equals(cleanRain) || 
            "---".equals(cleanRain) || "N/A".equalsIgnoreCase(cleanRain) ||
            "-".equals(cleanRain) || "".equals(cleanRain)) {
            return 0.0;
        }
        
        try {
            // Try to parse the rain value directly
            return Double.parseDouble(cleanRain);
        } catch (NumberFormatException e) {
            // If direct parsing fails, try to extract numeric part
            try {
                String numericStr = cleanRain.replaceAll("[^0-9.]", "");
                if (!numericStr.isEmpty() && !".".equals(numericStr)) {
                    return Double.parseDouble(numericStr);
                }
            } catch (NumberFormatException ex) {
                // If we still can't parse it, assume no rain
                return 0.0;
            }
        }
        
        return 0.0;
    }
}
