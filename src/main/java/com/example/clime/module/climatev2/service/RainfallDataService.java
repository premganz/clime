package com.example.clime.module.climatev2.service;

import com.example.clime.module.climatev2.model.RainfallRecord;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

@Service("rainfallDataServiceV2")
public class RainfallDataService {
    
    private List<RainfallRecord> rainfallData;
    
    public RainfallDataService() {
        loadRainfallData();
    }
    
    private void loadRainfallData() {
        rainfallData = new ArrayList<>();
        try {
            ClassPathResource resource = new ClassPathResource("chennai-monthly-rains.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
            
            String line;
            boolean isHeader = true;
            
            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false; // Skip header
                    continue;
                }
                
                String[] parts = line.split(",");
                if (parts.length >= 13) {
                    try {
                        int year = Integer.parseInt(parts[0]);
                        double jan = Double.parseDouble(parts[1]);
                        double feb = Double.parseDouble(parts[2]);
                        double mar = Double.parseDouble(parts[3]);
                        double april = Double.parseDouble(parts[4]);
                        double may = Double.parseDouble(parts[5]);
                        double june = Double.parseDouble(parts[6]);
                        double july = Double.parseDouble(parts[7]);
                        double aug = Double.parseDouble(parts[8]);
                        double sept = Double.parseDouble(parts[9]);
                        double oct = Double.parseDouble(parts[10]);
                        double nov = Double.parseDouble(parts[11]);
                        double dec = Double.parseDouble(parts[12]);
                        double total = Double.parseDouble(parts[13]);
                        
                        RainfallRecord record = new RainfallRecord(year, jan, feb, mar, april, may, june, 
                                                                 july, aug, sept, oct, nov, dec, total);
                        rainfallData.add(record);
                    } catch (NumberFormatException e) {
                        System.err.println("Error parsing line: " + line);
                    }
                }
            }
            reader.close();
            
            System.out.println("Loaded " + rainfallData.size() + " rainfall records");
        } catch (IOException e) {
            System.err.println("Error loading rainfall data: " + e.getMessage());
        }
    }
    
    public List<RainfallRecord> getAllData() {
        return new ArrayList<>(rainfallData);
    }
    
    public List<RainfallRecord> getDataByYear(int year) {
        return rainfallData.stream()
                .filter(record -> record.getYear() == year)
                .collect(Collectors.toList());
    }
    
    public List<RainfallRecord> getDataByYearRange(int startYear, int endYear) {
        return rainfallData.stream()
                .filter(record -> record.getYear() >= startYear && record.getYear() <= endYear)
                .collect(Collectors.toList());
    }
    
    public List<RainfallRecord> getDataByMonth(int month) {
        return rainfallData.stream()
                .filter(record -> record.getRainfallForMonth(month) > 0)
                .collect(Collectors.toList());
    }
    
    public double getAverageRainfallForMonth(int month) {
        return rainfallData.stream()
                .mapToDouble(record -> record.getRainfallForMonth(month))
                .average()
                .orElse(0.0);
    }
    
    public double getAverageAnnualRainfall() {
        return rainfallData.stream()
                .mapToDouble(RainfallRecord::getTotal)
                .average()
                .orElse(0.0);
    }
    
    public RainfallRecord getMaxRainfallYear() {
        return rainfallData.stream()
                .max(Comparator.comparing(RainfallRecord::getTotal))
                .orElse(null);
    }
    
    public RainfallRecord getMinRainfallYear() {
        return rainfallData.stream()
                .min(Comparator.comparing(RainfallRecord::getTotal))
                .orElse(null);
    }
    
    public Map<String, Object> getBasicStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        if (rainfallData.isEmpty()) {
            return stats;
        }
        
        stats.put("totalRecords", rainfallData.size());
        stats.put("yearRange", rainfallData.get(0).getYear() + " - " + rainfallData.get(rainfallData.size() - 1).getYear());
        stats.put("averageAnnualRainfall", Math.round(getAverageAnnualRainfall() * 100.0) / 100.0);
        
        RainfallRecord maxYear = getMaxRainfallYear();
        RainfallRecord minYear = getMinRainfallYear();
        
        if (maxYear != null) {
            stats.put("highestRainfallYear", maxYear.getYear() + " (" + Math.round(maxYear.getTotal() * 100.0) / 100.0 + "mm)");
        }
        if (minYear != null) {
            stats.put("lowestRainfallYear", minYear.getYear() + " (" + Math.round(minYear.getTotal() * 100.0) / 100.0 + "mm)");
        }
        
        // Monthly averages
        Map<String, Double> monthlyAverages = new HashMap<>();
        for (int month = 1; month <= 12; month++) {
            monthlyAverages.put(RainfallRecord.getMonthName(month), 
                    Math.round(getAverageRainfallForMonth(month) * 100.0) / 100.0);
        }
        stats.put("monthlyAverages", monthlyAverages);
        
        return stats;
    }
    
    public String generateRainfallTableHtml(List<RainfallRecord> records) {
        StringBuilder html = new StringBuilder();
        html.append("<div class='table-responsive'>");
        html.append("<table class='table table-striped table-sm'>");
        html.append("<thead class='table-dark'>");
        html.append("<tr>");
        html.append("<th>Year</th><th>Jan</th><th>Feb</th><th>Mar</th><th>Apr</th>");
        html.append("<th>May</th><th>Jun</th><th>Jul</th><th>Aug</th><th>Sep</th>");
        html.append("<th>Oct</th><th>Nov</th><th>Dec</th><th>Total</th>");
        html.append("</tr>");
        html.append("</thead>");
        html.append("<tbody>");
        
        for (RainfallRecord record : records) {
            html.append("<tr>");
            html.append("<td><strong>").append(record.getYear()).append("</strong></td>");
            html.append("<td>").append(String.format("%.1f", record.getJan())).append("</td>");
            html.append("<td>").append(String.format("%.1f", record.getFeb())).append("</td>");
            html.append("<td>").append(String.format("%.1f", record.getMar())).append("</td>");
            html.append("<td>").append(String.format("%.1f", record.getApril())).append("</td>");
            html.append("<td>").append(String.format("%.1f", record.getMay())).append("</td>");
            html.append("<td>").append(String.format("%.1f", record.getJune())).append("</td>");
            html.append("<td>").append(String.format("%.1f", record.getJuly())).append("</td>");
            html.append("<td>").append(String.format("%.1f", record.getAug())).append("</td>");
            html.append("<td>").append(String.format("%.1f", record.getSept())).append("</td>");
            html.append("<td>").append(String.format("%.1f", record.getOct())).append("</td>");
            html.append("<td>").append(String.format("%.1f", record.getNov())).append("</td>");
            html.append("<td>").append(String.format("%.1f", record.getDec())).append("</td>");
            html.append("<td><strong>").append(String.format("%.1f", record.getTotal())).append("</strong></td>");
            html.append("</tr>");
        }
        
        html.append("</tbody>");
        html.append("</table>");
        html.append("</div>");
        
        return html.toString();
    }
}
