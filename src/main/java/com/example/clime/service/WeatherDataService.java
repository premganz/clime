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
            // Skip lines that are too short or don't start with a digit
            if (line.length() < 20 || !Character.isDigit(line.charAt(0))) {
                return null;
            }
            
            // Use robust token-based parsing for all formats
            return parseDataLineRobust(line, year, month);
            
        } catch (Exception e) {
            System.err.println("Error parsing line: " + line);
            System.err.println("Error: " + e.getMessage());
            return null;
        }
    }
    
    private WeatherRecord parseDataLineRobust(String line, int year, int month) {
        // Normalize whitespace and split into tokens
        String[] tokens = line.trim().replaceAll("\\s+", " ").split("\\s+");
        
        if (tokens.length < 6) {
            System.out.println("⚠️ Skipping line with insufficient tokens (" + tokens.length + "): " + line);
            return null;
        }
        
        try {
            String day = tokens[0];
            
            // Extract temperature values - they're typically tokens 1-4
            String meanTemp = safeParse(tokens, 1, "0");
            String highTemp = safeParse(tokens, 2, "0");
            String lowTemp = safeParse(tokens, 3, "0");
            
            // For precipitation, we need to find the right token by looking for a decimal value
            // that could be rain (typically small positive number or T for trace)
            String rain = findRainValue(tokens);
            
            // Set other values with defaults
            String windAvg = "0";
            String windHi = "0";
            String meanBarom = "1013.0";
            String meanHum = "70";
            
            // Try to extract additional data if available
            if (tokens.length > 6) {
                // Look for barometric pressure (typically 1000+ value)
                for (int i = 5; i < tokens.length; i++) {
                    if (tokens[i].matches("\\d{4}\\.\\d+") && tokens[i].startsWith("10")) {
                        meanBarom = tokens[i];
                        break;
                    }
                }
                
                // Look for humidity (typically 0-100 value)
                for (int i = 5; i < tokens.length; i++) {
                    if (tokens[i].matches("\\d{1,3}") && Integer.parseInt(tokens[i]) <= 100) {
                        meanHum = tokens[i];
                        break;
                    }
                }
            }
            
            // Debug output for first few days
            if (Integer.parseInt(day) <= 3) {
                System.out.println("🔍 [ROBUST] Parsing day " + day + " of " + year + "/" + month + ":");
                System.out.println("  Rain: '" + rain + "'");
                System.out.println("  Temps: mean=" + meanTemp + ", high=" + highTemp + ", low=" + lowTemp);
                System.out.println("  Tokens: " + Arrays.toString(tokens));
            }
            
            return new WeatherRecord(
                String.valueOf(year), String.valueOf(month), day,
                meanTemp, highTemp, "12:00pm", lowTemp, "6:00am",
                "0", "0", rain, windAvg, windHi,
                "12:00pm", "SE", meanBarom, meanHum
            );
            
        } catch (Exception e) {
            System.out.println("❌ Error parsing tokens for line: " + line + " - " + e.getMessage());
            return null;
        }
    }
    
    private String safeParse(String[] tokens, int index, String defaultValue) {
        if (index >= tokens.length) return defaultValue;
        String value = tokens[index];
        if (value.matches("-?\\d+(\\.\\d+)?")) {
            return value;
        }
        return defaultValue;
    }
    
    private String findRainValue(String[] tokens) {
        // Look for precipitation value - typically a small decimal or "T" for trace
        for (int i = 4; i < tokens.length; i++) {
            String token = tokens[i];
            
            // Check for trace rain
            if ("T".equals(token)) {
                return "0.01"; // Trace amount
            }
            
            // Check for decimal rain value (typically 0.00 to 10.00)
            if (token.matches("\\d{1,2}\\.\\d{2}")) {
                double rainValue = Double.parseDouble(token);
                if (rainValue >= 0.0 && rainValue <= 20.0) { // Reasonable rain range
                    return token;
                }
            }
            
            // Check for integer rain value
            if (token.matches("\\d{1,2}") && !token.startsWith("10")) { // Not barometric pressure
                int rainValue = Integer.parseInt(token);
                if (rainValue >= 0 && rainValue <= 20) {
                    return token + ".0";
                }
            }
        }
        
        return "0.0"; // Default to no rain
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
