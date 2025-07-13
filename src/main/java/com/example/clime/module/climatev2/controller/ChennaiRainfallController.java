package com.example.clime.module.climatev2.controller;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Chennai Rainfall Plot Controller
 * 
 * Provides REST endpoints for Chennai rainfall data visualization
 * - Reads CSV data from src/main/resources/chennai_monthly_rainfall.csv
 * - Aggregates yearly rainfall data
 * - Generates PNG plots using XChart library
 * 
 * Usage:
 * GET /climateV2/chennai-rainfall-plot - Returns PNG image of annual rainfall trend
 * GET /climateV2/chennai-rainfall-html - Returns HTML page with embedded plot
 */
@RestController
@RequestMapping("/climateV2")
public class ChennaiRainfallController {

    /**
     * Generate Chennai rainfall plot as PNG image
     * Aggregates monthly data to yearly totals and creates a line chart
     * 
     * @return ResponseEntity containing PNG image bytes
     */
    @GetMapping("/chennai-rainfall-plot")
    public ResponseEntity<byte[]> getChennaiRainfallPlot() {
        try {
            // Read and aggregate data
            Map<Integer, Double> yearlyRainfall = readAndAggregateData();
            
            if (yearlyRainfall.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("No data available".getBytes());
            }

            // Create chart
            XYChart chart = new XYChartBuilder()
                .width(800)
                .height(600)
                .title("Chennai Annual Rainfall Trend (Last 25 Years)")
                .xAxisTitle("Year")
                .yAxisTitle("Rainfall (mm)")
                .build();

            // Chart styling
            chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);
            chart.getStyler().setDefaultSeriesRenderStyle(org.knowm.xchart.XYSeries.XYSeriesRenderStyle.Line);
            chart.getStyler().setMarkerSize(6);

            // Prepare data for chart
            List<Integer> years = new ArrayList<>(yearlyRainfall.keySet());
            Collections.sort(years);
            
            List<Double> yearsDouble = new ArrayList<>();
            List<Double> rainfallValues = new ArrayList<>();
            
            for (Integer year : years) {
                yearsDouble.add(year.doubleValue());
                rainfallValues.add(yearlyRainfall.get(year));
            }

            // Add data series
            chart.addSeries("Annual Rainfall", yearsDouble, rainfallValues);

            // Convert chart to PNG bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BitmapEncoder.saveBitmap(chart, baos, BitmapEncoder.BitmapFormat.PNG);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentLength(baos.size());
            
            return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(("Error generating plot: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Generate HTML page with embedded Chennai rainfall plot
     * 
     * @return ResponseEntity containing HTML with embedded base64 image
     */
    @GetMapping("/chennai-rainfall-html")
    public ResponseEntity<String> getChennaiRainfallHtml() {
        try {
            // Read and aggregate data
            Map<Integer, Double> yearlyRainfall = readAndAggregateData();
            
            if (yearlyRainfall.isEmpty()) {
                return ResponseEntity.ok("<div class='alert alert-danger'>No data available</div>");
            }

            // Create chart
            XYChart chart = new XYChartBuilder()
                .width(800)
                .height(600)
                .title("Chennai Annual Rainfall Trend (Last 25 Years)")
                .xAxisTitle("Year")
                .yAxisTitle("Rainfall (mm)")
                .build();

            // Chart styling
            chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);
            chart.getStyler().setDefaultSeriesRenderStyle(org.knowm.xchart.XYSeries.XYSeriesRenderStyle.Line);
            chart.getStyler().setMarkerSize(6);

            // Prepare data for chart
            List<Integer> years = new ArrayList<>(yearlyRainfall.keySet());
            Collections.sort(years);
            
            List<Double> yearsDouble = new ArrayList<>();
            List<Double> rainfallValues = new ArrayList<>();
            
            for (Integer year : years) {
                yearsDouble.add(year.doubleValue());
                rainfallValues.add(yearlyRainfall.get(year));
            }

            // Add data series
            chart.addSeries("Annual Rainfall", yearsDouble, rainfallValues);

            // Convert chart to base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BitmapEncoder.saveBitmap(chart, baos, BitmapEncoder.BitmapFormat.PNG);
            String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());

            // Generate HTML response
            StringBuilder html = new StringBuilder();
            html.append("<div style='padding: 20px; background: #f8f9fa; border-radius: 8px; margin: 20px 0;'>");
            html.append("<h3>🌧️ Chennai Rainfall Analysis</h3>");
            html.append("<p>Annual rainfall trend for Chennai over the last 25 years (1997-2021)</p>");
            html.append("<div style='text-align: center; margin: 20px 0;'>");
            html.append("<img src='data:image/png;base64,").append(base64Image).append("' ");
            html.append("alt='Chennai Annual Rainfall Chart' style='max-width: 100%; height: auto; border: 1px solid #ddd;'/>");
            html.append("</div>");
            
            // Add summary statistics
            double avgRainfall = yearlyRainfall.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double maxRainfall = yearlyRainfall.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            double minRainfall = yearlyRainfall.values().stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
            
            html.append("<div class='row mt-3'>");
            html.append("<div class='col-md-4 text-center'>");
            html.append("<h5>Average Annual Rainfall</h5>");
            html.append("<p class='h4 text-primary'>").append(String.format("%.1f mm", avgRainfall)).append("</p>");
            html.append("</div>");
            html.append("<div class='col-md-4 text-center'>");
            html.append("<h5>Highest Annual Rainfall</h5>");
            html.append("<p class='h4 text-success'>").append(String.format("%.1f mm", maxRainfall)).append("</p>");
            html.append("</div>");
            html.append("<div class='col-md-4 text-center'>");
            html.append("<h5>Lowest Annual Rainfall</h5>");
            html.append("<p class='h4 text-warning'>").append(String.format("%.1f mm", minRainfall)).append("</p>");
            html.append("</div>");
            html.append("</div>");
            html.append("</div>");
            
            return ResponseEntity.ok(html.toString());
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok("<div class='alert alert-danger'>Error generating plot: " + e.getMessage() + "</div>");
        }
    }

    /**
     * Read chennai_monthly_rainfall.csv and aggregate by year
     * 
     * @return Map of year -> total annual rainfall
     */
    private Map<Integer, Double> readAndAggregateData() {
        Map<Integer, Double> yearlyRainfall = new HashMap<>();
        
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("chennai_monthly_rainfall.csv")) {
            if (is == null) {
                throw new RuntimeException("Chennai rainfall data file not found");
            }
            
            Scanner scanner = new Scanner(is);
            
            // Skip header
            if (scanner.hasNextLine()) {
                scanner.nextLine();
            }
            
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;
                
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    try {
                        int year = Integer.parseInt(parts[0].trim());
                        double rainfall = Double.parseDouble(parts[2].trim());
                        
                        yearlyRainfall.merge(year, rainfall, Double::sum);
                    } catch (NumberFormatException e) {
                        // Skip invalid lines
                        System.err.println("Skipping invalid line: " + line);
                    }
                }
            }
            scanner.close();
            
        } catch (IOException e) {
            throw new RuntimeException("Error reading Chennai rainfall data", e);
        }
        
        return yearlyRainfall;
    }
}