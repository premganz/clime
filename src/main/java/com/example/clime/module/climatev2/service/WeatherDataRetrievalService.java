package com.example.clime.module.climatev2.service;

import com.example.clime.module.climatev2.model.WeatherRecord;
import com.opencsv.CSVReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service("weatherDataRetrievalServiceV2")
public class WeatherDataRetrievalService {
    
    @Value("${weather.unscramble.key}")
    private String unscrambleKey;

    public List<WeatherRecord> getWeatherData(String year, String month, String providedKey) {
        if (!unscrambleKey.equals(providedKey)) {
            throw new SecurityException("Invalid unscramble key");
        }
        
        List<WeatherRecord> filteredRecords = new ArrayList<>();
        
        try {
            ClassPathResource resource = new ClassPathResource("scrambled_weather_data.csv");
            try (CSVReader csvReader = new CSVReader(new InputStreamReader(resource.getInputStream()))) {
                
                String[] header = csvReader.readNext(); // Skip header
                String[] line;
                
                while ((line = csvReader.readNext()) != null) {
                    if (line.length >= 20) {
                        WeatherRecord record = new WeatherRecord();
                        record.setYear(line[1]);
                        record.setMonth(line[2]);
                        record.setDay(line[3]);
                        record.setMeanTemp(line[4]);
                        record.setHighTemp(line[5]);
                        record.setHighTime(line[6]);
                        record.setLowTemp(line[7]);
                        record.setLowTime(line[8]);
                        record.setHeatDegDays(line[9]);
                        record.setCoolDegDays(line[10]);
                        record.setRain(line[11]);
                        record.setWindAvg(line[12]);
                        record.setWindHi(line[13]);
                        record.setWindHiTime(line[14]);
                        record.setDomDir(line[15]);
                        record.setMeanBarom(line[16]);
                        record.setMeanHum(line[17]);
                        record.setFlagged(line[18]);
                        record.setAnomalyNote(line[19]);
                        
                        // Filter by year and month
                        if (record.getYear().equals(year) && record.getMonth().equals(month)) {
                            filteredRecords.add(record);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading weather data: " + e.getMessage());
        }
        
        // Sort by day (ascending)
        filteredRecords.sort(Comparator.comparing(record -> Integer.parseInt(record.getDay())));
        
        return filteredRecords;
    }
    
    public String generateHtmlTable(List<WeatherRecord> records) {
        StringBuilder html = new StringBuilder();
        html.append("<div class='weather-data'>");
        html.append("<h3>Weather Data V2</h3>");
        html.append("<table class='table table-striped table-bordered'>");
        html.append("<thead class='table-dark'>");
        html.append("<tr>");
        html.append("<th>Day</th><th>Mean Temp</th><th>High Temp</th><th>High Time</th>");
        html.append("<th>Low Temp</th><th>Low Time</th><th>Rain</th><th>Wind Avg</th>");
        html.append("<th>Wind Hi</th><th>Dom Dir</th><th>Barometric</th><th>Humidity</th>");
        html.append("<th>Flagged</th><th>Anomaly Note</th>");
        html.append("</tr>");
        html.append("</thead>");
        html.append("<tbody>");
        
        for (WeatherRecord record : records) {
            String rowClass = "Y".equals(record.getFlagged()) ? "table-warning" : "";
            html.append("<tr class='").append(rowClass).append("'>");
            html.append("<td>").append(escape(record.getDay())).append("</td>");
            html.append("<td>").append(escape(record.getMeanTemp())).append("</td>");
            html.append("<td>").append(escape(record.getHighTemp())).append("</td>");
            html.append("<td>").append(escape(record.getHighTime())).append("</td>");
            html.append("<td>").append(escape(record.getLowTemp())).append("</td>");
            html.append("<td>").append(escape(record.getLowTime())).append("</td>");
            html.append("<td>").append(escape(record.getRain())).append("</td>");
            html.append("<td>").append(escape(record.getWindAvg())).append("</td>");
            html.append("<td>").append(escape(record.getWindHi())).append("</td>");
            html.append("<td>").append(escape(record.getDomDir())).append("</td>");
            html.append("<td>").append(escape(record.getMeanBarom())).append("</td>");
            html.append("<td>").append(escape(record.getMeanHum())).append("</td>");
            html.append("<td>");
            if ("Y".equals(record.getFlagged())) {
                html.append("<span class='badge bg-warning'>⚠️ Y</span>");
            } else {
                html.append("<span class='badge bg-success'>✓ F</span>");
            }
            html.append("</td>");
            html.append("<td>").append(escape(record.getAnomalyNote())).append("</td>");
            html.append("</tr>");
        }
        
        html.append("</tbody>");
        html.append("</table>");
        html.append("</div>");
        
        return html.toString();
    }
    
    private String escape(String value) {
        if (value == null) return "";
        return value.replace("<", "&lt;").replace(">", "&gt;").replace("&", "&amp;");
    }
}
