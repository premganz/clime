package com.example.clime.module.climatev2.service;

import com.example.clime.module.climatev2.model.WeatherRecord;
import com.opencsv.CSVWriter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("weatherDataServiceV2")
public class WeatherDataService {
    
    private static final String BASE_URL = "https://xyzxyzxyzxyz.com/summary/";
    private static final Random random = new Random(42); // Fixed seed for consistent scrambling
    
    public void fetchAndProcessAllData() {
        List<WeatherRecord> allRecords = new ArrayList<>();
        
        // Fetch data from 2005_09 to 2025_06
        for (int year = 2005; year <= 2025; year++) {
            int startMonth = (year == 2005) ? 9 : 1;
            int endMonth = (year == 2025) ? 6 : 12;
            
            for (int month = startMonth; month <= endMonth; month++) {
                String yearMonth = String.format("%d_%02d", year, month);
                System.out.println("ClimateV2: Fetching data for: " + yearMonth);
                
                try {
                    List<WeatherRecord> records = fetchMonthData(year, month);
                    allRecords.addAll(records);
                    Thread.sleep(100); // Be nice to the server
                } catch (Exception e) {
                    System.err.println("ClimateV2: Error fetching data for " + yearMonth + ": " + e.getMessage());
                }
            }
        }
        
        // Detect anomalies
        detectAnomalies(allRecords);
        
        // Scramble and save
        scrambleAndSaveData(allRecords);
        
        System.out.println("ClimateV2: Data fetch and processing complete! Total records: " + allRecords.size());
    }
    
    private List<WeatherRecord> fetchMonthData(int year, int month) throws Exception {
        String yearMonth = String.format("%d_%02d", year, month);
        String url = BASE_URL + yearMonth;
        
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        
        if (connection.getResponseCode() != 200) {
            throw new Exception("HTTP " + connection.getResponseCode() + " for " + url);
        }
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()))) {
            
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            
            return parseWeatherData(content.toString(), year, month);
        }
    }
    
    private List<WeatherRecord> parseWeatherData(String html, int year, int month) {
        List<WeatherRecord> records = new ArrayList<>();
        
        // Pattern to match table rows
        Pattern rowPattern = Pattern.compile("<tr[^>]*>(.*?)</tr>", Pattern.DOTALL);
        Pattern cellPattern = Pattern.compile("<td[^>]*>(.*?)</td>", Pattern.DOTALL);
        
        Matcher rowMatcher = rowPattern.matcher(html);
        boolean isFirstRow = true;
        
        while (rowMatcher.find()) {
            if (isFirstRow) {
                isFirstRow = false;
                continue; // Skip header row
            }
            
            String rowContent = rowMatcher.group(1);
            Matcher cellMatcher = cellPattern.matcher(rowContent);
            
            List<String> cells = new ArrayList<>();
            while (cellMatcher.find()) {
                String cellContent = cellMatcher.group(1);
                cellContent = cellContent.replaceAll("<[^>]+>", ""); // Remove HTML tags
                cellContent = cellContent.replaceAll("&nbsp;", " ").trim();
                cells.add(cellContent);
            }
            
            if (cells.size() >= 16) {
                WeatherRecord record = new WeatherRecord(
                    String.valueOf(year),
                    String.valueOf(month),
                    cells.get(0), // day
                    cells.get(1), // meanTemp
                    cells.get(2), // highTemp
                    cells.get(3), // highTime
                    cells.get(4), // lowTemp
                    cells.get(5), // lowTime
                    cells.get(6), // heatDegDays
                    cells.get(7), // coolDegDays
                    cells.get(8), // rain
                    cells.get(9), // windAvg
                    cells.get(10), // windHi
                    cells.get(11), // windHiTime
                    cells.get(12), // domDir
                    cells.get(13), // meanBarom
                    cells.get(14)  // meanHum
                );
                records.add(record);
            }
        }
        
        return records;
    }
    
    private void detectAnomalies(List<WeatherRecord> records) {
        System.out.println("ClimateV2: Detecting anomalies in " + records.size() + " records...");
        
        for (WeatherRecord record : records) {
            List<String> anomalies = new ArrayList<>();
            
            // Check for temperature anomalies
            try {
                double meanTemp = Double.parseDouble(record.getMeanTemp());
                double highTemp = Double.parseDouble(record.getHighTemp());
                double lowTemp = Double.parseDouble(record.getLowTemp());
                
                if (meanTemp > 100 || meanTemp < -50) {
                    anomalies.add("Extreme mean temperature");
                }
                if (highTemp > 120 || highTemp < meanTemp) {
                    anomalies.add("Invalid high temperature");
                }
                if (lowTemp < -60 || lowTemp > meanTemp) {
                    anomalies.add("Invalid low temperature");
                }
                if (Math.abs(highTemp - lowTemp) > 60) {
                    anomalies.add("Extreme temperature range");
                }
            } catch (NumberFormatException e) {
                anomalies.add("Non-numeric temperature");
            }
            
            // Check for rainfall anomalies
            try {
                double rain = Double.parseDouble(record.getRain());
                if (rain > 10.0) {
                    anomalies.add("Heavy rainfall");
                }
                if (rain < 0) {
                    anomalies.add("Negative rainfall");
                }
            } catch (NumberFormatException e) {
                // Ignore non-numeric rain values
            }
            
            // Check for wind anomalies
            try {
                double windHi = Double.parseDouble(record.getWindHi());
                if (windHi > 100) {
                    anomalies.add("Extreme wind speed");
                }
            } catch (NumberFormatException e) {
                // Ignore non-numeric wind values
            }
            
            // Set flags
            if (!anomalies.isEmpty()) {
                record.setFlagged("Y");
                record.setAnomalyNote(String.join("; ", anomalies));
            } else {
                record.setFlagged("F");
                record.setAnomalyNote("");
            }
        }
        
        long flaggedCount = records.stream().mapToLong(r -> "Y".equals(r.getFlagged()) ? 1 : 0).sum();
        System.out.println("ClimateV2: Found " + flaggedCount + " anomalous records");
    }
    
    private void scrambleAndSaveData(List<WeatherRecord> records) {
        try {
            String outputPath = "src/main/resources/scrambled_weather_data_v2.csv";
            
            try (CSVWriter writer = new CSVWriter(new FileWriter(outputPath))) {
                // Write header
                String[] header = {"id", "year", "month", "day", "meanTemp", "highTemp", "highTime", 
                                 "lowTemp", "lowTime", "heatDegDays", "coolDegDays", "rain", 
                                 "windAvg", "windHi", "windHiTime", "domDir", "meanBarom", 
                                 "meanHum", "flagged", "anomalyNote"};
                writer.writeNext(header);
                
                // Scramble order
                List<WeatherRecord> scrambledRecords = new ArrayList<>(records);
                Collections.shuffle(scrambledRecords, random);
                
                // Write data
                int id = 1;
                for (WeatherRecord record : scrambledRecords) {
                    String[] row = {
                        String.valueOf(id++),
                        record.getYear(),
                        record.getMonth(),
                        record.getDay(),
                        record.getMeanTemp(),
                        record.getHighTemp(),
                        record.getHighTime(),
                        record.getLowTemp(),
                        record.getLowTime(),
                        record.getHeatDegDays(),
                        record.getCoolDegDays(),
                        record.getRain(),
                        record.getWindAvg(),
                        record.getWindHi(),
                        record.getWindHiTime(),
                        record.getDomDir(),
                        record.getMeanBarom(),
                        record.getMeanHum(),
                        record.getFlagged(),
                        record.getAnomalyNote()
                    };
                    writer.writeNext(row);
                }
            }
            
            System.out.println("ClimateV2: Scrambled data saved to: " + outputPath);
            
        } catch (Exception e) {
            System.err.println("ClimateV2: Error saving scrambled data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
