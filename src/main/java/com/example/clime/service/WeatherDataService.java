package com.example.clime.service;

import com.example.clime.model.WeatherRecord;
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

@Service
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
                System.out.println("Fetching data for: " + yearMonth);
                
                try {
                    List<WeatherRecord> records = fetchMonthData(year, month);
                    allRecords.addAll(records);
                    Thread.sleep(100); // Be nice to the server
                } catch (Exception e) {
                    System.err.println("Error fetching data for " + yearMonth + ": " + e.getMessage());
                }
            }
        }
        
        // Detect anomalies
        detectAnomalies(allRecords);
        
        // Scramble and save
        scrambleAndSaveData(allRecords);
        
        System.out.println("Total records processed: " + allRecords.size());
        System.out.println("Data saved to scrambled_weather_data.csv");
    }
    
    private List<WeatherRecord> fetchMonthData(int year, int month) throws IOException {
        String yearMonth = String.format("%d_%02d", year, month);
        String urlString = BASE_URL + yearMonth + ".txt";
        
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(10000);
        
        List<WeatherRecord> records = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            boolean dataStarted = false;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // Skip header lines until we find the data separator
                if (line.contains("---")) {
                    dataStarted = true;
                    continue;
                }
                
                if (dataStarted && !line.isEmpty() && Character.isDigit(line.charAt(0))) {
                    WeatherRecord record = parseDataLine(line, year, month);
                    if (record != null) {
                        records.add(record);
                    }
                }
            }
        }
        
        return records;
    }
    
    private WeatherRecord parseDataLine(String line, int year, int month) {
        try {
            // Parse the fixed-width format data according to the actual format
            if (line.length() < 70) {
                return null; // Skip incomplete lines
            }
            
            // Fixed-width parsing based on actual data format:
            // DAY  TEMP  HIGH   TIME     LOW   TIME    RAIN   AVG  HI  TIME     DIR   BAROM   HUM     RUN
            String day = extractField(line, 0, 3).trim();
            String meanTemp = extractField(line, 5, 9).trim();
            String highTemp = extractField(line, 10, 15).trim();
            String highTime = extractField(line, 16, 24).trim();
            String lowTemp = extractField(line, 25, 30).trim();
            String lowTime = extractField(line, 31, 39).trim();
            String rain = extractField(line, 42, 46).trim();
            String windAvg = extractField(line, 49, 52).trim();
            String windHi = extractField(line, 53, 56).trim();
            String windHiTime = extractField(line, 57, 66).trim();
            String domDir = extractField(line, 69, 73).trim();
            String meanBarom = "";
            String meanHum = "";
            String windRun = "";
            
            // Extract barometric pressure, humidity, and wind run if available
            if (line.length() > 73) {
                String remaining = line.substring(73).trim();
                String[] parts = remaining.split("\\s+");
                if (parts.length >= 1) {
                    meanBarom = parts[0];
                }
                if (parts.length >= 2) {
                    meanHum = parts[1];
                }
                if (parts.length >= 3) {
                    windRun = parts[2];
                }
            }
            
            return new WeatherRecord(
                String.valueOf(year), String.valueOf(month), day,
                meanTemp, highTemp, highTime, lowTemp, lowTime,
                "0", "0", rain, windAvg, windHi,  // Heat/Cool deg days not in this format
                windHiTime, domDir, meanBarom, meanHum
            );
            
        } catch (Exception e) {
            System.err.println("Error parsing line: " + line);
            System.err.println("Line length: " + line.length());
            return null;
        }
    }
    
    private String extractField(String line, int start, int end) {
        if (start >= line.length()) return "";
        if (end > line.length()) end = line.length();
        return line.substring(start, end);
    }
    
    private void detectAnomalies(List<WeatherRecord> records) {
        for (WeatherRecord record : records) {
            List<String> anomalies = new ArrayList<>();
            
            // Check for suspicious temperature values - be more lenient
            try {
                if (!isZeroOrEmpty(record.getMeanTemp()) && !isZeroOrEmpty(record.getHighTemp()) && !isZeroOrEmpty(record.getLowTemp())) {
                    double meanTemp = parseDouble(record.getMeanTemp());
                    double highTemp = parseDouble(record.getHighTemp());
                    double lowTemp = parseDouble(record.getLowTemp());
                    
                    // More realistic temperature checks for Chennai weather
                    if (meanTemp < 15 || meanTemp > 45) {
                        anomalies.add("Unusual mean temperature: " + meanTemp + "°C");
                    }
                    if (highTemp < lowTemp) {
                        anomalies.add("High temp lower than low temp");
                    }
                    if (highTemp - lowTemp > 25) {
                        anomalies.add("Extreme temperature range: " + String.format("%.1f", (highTemp - lowTemp)) + "°C");
                    }
                }
            } catch (NumberFormatException e) {
                // Skip temperature anomaly check if parsing fails
            }
            
            // More lenient barometric pressure check
            if (!isZeroOrEmpty(record.getMeanBarom())) {
                try {
                    String baromStr = record.getMeanBarom().replaceAll("[^0-9.]", "");
                    if (!baromStr.isEmpty()) {
                        double barom = Double.parseDouble(baromStr);
                        // Chennai typical barometric pressure: 1000-1020 hPa
                        // But allow wider range to account for data format variations
                        if (barom > 50 && (barom < 980 || barom > 1040)) {
                            anomalies.add("Unusual barometric pressure: " + barom + " hPa");
                        }
                    }
                } catch (NumberFormatException e) {
                    // Skip barometric pressure check if parsing fails
                }
            }
            
            // More lenient humidity check
            if (!isZeroOrEmpty(record.getMeanHum())) {
                try {
                    String humStr = record.getMeanHum().replaceAll("[^0-9.]", "");
                    if (!humStr.isEmpty()) {
                        double humidity = Double.parseDouble(humStr);
                        if (humidity < 10 || humidity > 100) {
                            anomalies.add("Unusual humidity: " + humidity + "%");
                        }
                    }
                } catch (NumberFormatException e) {
                    // Skip humidity check if parsing fails
                }
            }
            
            // Check for completely missing essential data
            if (isZeroOrEmpty(record.getMeanTemp()) && isZeroOrEmpty(record.getHighTemp()) && isZeroOrEmpty(record.getLowTemp())) {
                anomalies.add("Missing temperature data");
            }
            
            // Check for suspicious rain values
            if (!isZeroOrEmpty(record.getRain())) {
                try {
                    String rainStr = record.getRain().replaceAll("[^0-9.]", "");
                    if (!rainStr.isEmpty()) {
                        double rain = Double.parseDouble(rainStr);
                        if (rain > 200) {
                            anomalies.add("Extreme rainfall: " + rain + "mm");
                        }
                    }
                } catch (NumberFormatException e) {
                    // Skip rain check if parsing fails
                }
            }
            
            // Set anomaly flags
            if (!anomalies.isEmpty()) {
                record.setFlagged("Y");
                record.setAnomalyNote(String.join("; ", anomalies));
            } else {
                record.setFlagged("F");
                record.setAnomalyNote("");
            }
        }
    }
    
    private boolean isZeroOrEmpty(String value) {
        return StringUtils.isBlank(value) || "0".equals(value.trim()) || 
               "0.0".equals(value.trim()) || "---".equals(value.trim()) ||
               "N/A".equals(value.trim().toUpperCase());
    }
    
    private double parseDouble(String value) throws NumberFormatException {
        if (StringUtils.isBlank(value)) {
            throw new NumberFormatException("Empty value");
        }
        return Double.parseDouble(value.trim());
    }
    
    private void scrambleAndSaveData(List<WeatherRecord> records) {
        try {
            // Create resources directory if it doesn't exist
            Files.createDirectories(Paths.get("src/main/resources"));
            
            // Scramble the data (shuffle records)
            Collections.shuffle(records, random);
            
            // Write to CSV file
            try (FileWriter writer = new FileWriter("src/main/resources/scrambled_weather_data.csv");
                 CSVWriter csvWriter = new CSVWriter(writer)) {
                
                // Write header
                String[] header = {
                    "scrambled_id", "year", "month", "day", "mean_temp", "high_temp", "high_time", 
                    "low_temp", "low_time", "heat_deg_days", "cool_deg_days", "rain", 
                    "wind_avg", "wind_hi", "wind_hi_time", "dom_dir", "mean_barom", 
                    "mean_hum", "flagged", "anomaly_note"
                };
                csvWriter.writeNext(header);
                
                // Write data with scrambled IDs
                for (int i = 0; i < records.size(); i++) {
                    WeatherRecord record = records.get(i);
                    String[] row = {
                        generateScrambledId(i),
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
                    csvWriter.writeNext(row);
                }
            }
        } catch (IOException e) {
            System.err.println("Error saving scrambled data: " + e.getMessage());
        }
    }
    
    private String generateScrambledId(int index) {
        // Generate a scrambled ID using the index and key
        int scrambledIndex = (index * 17 + 42) % 100000;
        return String.format("SC%05d", scrambledIndex);
    }
    
    // Remove this method since we don't want to expose the key
    // public String getUnscrambleKey() {
    //     return SCRAMBLE_KEY;
    // }
}
