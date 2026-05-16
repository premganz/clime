package com.example.clime.module.climatev2.service;

import com.example.clime.module.climatev2.model.RainfallRecord;
import com.opencsv.CSVReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

@Service("kwsRainfallDataService")
public class KwsRainfallDataService {
    
    private static final String KWS_CSV_FILE = "kws-chennai-rainfall-2000-2025.csv";
    private List<RainfallRecord> kwsRainfallData;
    private boolean dataLoaded = false;
    private String lastError = null;
    
    @PostConstruct
    public void init() {
        // Load data at startup for performance optimization
        try {
            loadKwsRainfallData();
        } catch (Exception e) {
            System.err.println("Warning: Failed to load KWS rainfall data at startup: " + e.getMessage());
            lastError = "Failed to load KWS rainfall data: " + e.getMessage();
        }
    }
    
    public List<RainfallRecord> getAllData() throws Exception {
        if (!dataLoaded) {
            loadKwsRainfallData();
        }
        return new ArrayList<>(kwsRainfallData);
    }
    
    public List<RainfallRecord> getDataByYear(int year) throws Exception {
        List<RainfallRecord> allData = getAllData();
        return allData.stream()
                .filter(record -> record.getYear() == year)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    public List<RainfallRecord> getDataByYearRange(int startYear, int endYear) throws Exception {
        List<RainfallRecord> allData = getAllData();
        return allData.stream()
                .filter(record -> record.getYear() >= startYear && record.getYear() <= endYear)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    public String generateRainfallTableHtml(List<RainfallRecord> records) {
        StringBuilder html = new StringBuilder();
        html.append("<div class='table-responsive'>");
        html.append("<div class='alert alert-info'>");
        html.append("<strong>Data Source:</strong> KWS Chennai Local CSV Data (kws-chennai-rainfall-2000-2025.csv)");
        html.append("<br><small>Note: Data extracted from KWS Chennai website and stored locally for reliable access</small>");
        html.append("</div>");
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
    
    public String getLastError() {
        return lastError;
    }
    
    public boolean isDataLoaded() {
        return dataLoaded;
    }
    
    private void loadKwsRainfallData() throws Exception {
        kwsRainfallData = new ArrayList<>();
        lastError = null;
        
        try {
            System.out.println("Loading KWS rainfall data from local CSV: " + KWS_CSV_FILE);
            
            ClassPathResource resource = new ClassPathResource(KWS_CSV_FILE);
            if (!resource.exists()) {
                throw new IOException("KWS CSV file not found: " + KWS_CSV_FILE);
            }
            
            try (CSVReader csvReader = new CSVReader(new InputStreamReader(resource.getInputStream()))) {
                String[] headers = csvReader.readNext(); // Skip header line
                
                if (headers == null || headers.length < 13) {
                    throw new Exception("Invalid CSV format: expected at least 13 columns (Year + 12 months)");
                }
                
                String[] line;
                while ((line = csvReader.readNext()) != null) {
                    if (line.length >= 13) {
                        RainfallRecord record = parseCsvRow(line);
                        if (record != null) {
                            kwsRainfallData.add(record);
                        }
                    }
                }
            }
            
            if (kwsRainfallData.isEmpty()) {
                throw new Exception("No valid rainfall data found in CSV file");
            }
            
            // Sort by year for consistent ordering
            kwsRainfallData.sort(Comparator.comparing(RainfallRecord::getYear));
            
            dataLoaded = true;
            System.out.println("Successfully loaded " + kwsRainfallData.size() + " KWS rainfall records from CSV");
            
        } catch (IOException e) {
            lastError = "Failed to read KWS CSV file: " + e.getMessage();
            System.err.println("Error loading KWS data: " + lastError);
            throw new Exception(lastError, e);
        } catch (Exception e) {
            lastError = "Error parsing KWS CSV data: " + e.getMessage();
            System.err.println("Error loading KWS data: " + lastError);
            throw e;
        }
    }
    
    private RainfallRecord parseCsvRow(String[] row) {
        try {
            int year = Integer.parseInt(row[0].trim());
            
            // Parse monthly rainfall values (columns 1-12)
            double jan = Double.parseDouble(row[1].trim());
            double feb = Double.parseDouble(row[2].trim());
            double mar = Double.parseDouble(row[3].trim());
            double apr = Double.parseDouble(row[4].trim());
            double may = Double.parseDouble(row[5].trim());
            double jun = Double.parseDouble(row[6].trim());
            double jul = Double.parseDouble(row[7].trim());
            double aug = Double.parseDouble(row[8].trim());
            double sep = Double.parseDouble(row[9].trim());
            double oct = Double.parseDouble(row[10].trim());
            double nov = Double.parseDouble(row[11].trim());
            double dec = Double.parseDouble(row[12].trim());
            
            // Parse total if available (column 13), otherwise calculate
            double total;
            if (row.length > 13 && !row[13].trim().isEmpty()) {
                total = Double.parseDouble(row[13].trim());
            } else {
                total = jan + feb + mar + apr + may + jun + jul + aug + sep + oct + nov + dec;
            }
            
            return new RainfallRecord(year, jan, feb, mar, apr, may, jun, jul, aug, sep, oct, nov, dec, total);
            
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            System.err.println("Error parsing CSV row: " + Arrays.toString(row) + " - " + e.getMessage());
            return null;
        }
    }
}