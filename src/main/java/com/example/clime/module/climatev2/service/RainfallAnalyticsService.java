package com.example.clime.module.climatev2.service;

import com.example.clime.module.climatev2.model.RainfallRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service("rainfallAnalyticsServiceV2")
public class RainfallAnalyticsService {
    @Autowired
    @Qualifier("rainfallDataServiceV2")
    private RainfallDataService rainfallDataService;

    /**
     * Generate a yearly rainfall line graph as SVG (no JS, fits analytics dashboard).
     * @return HTML string with SVG chart
     */
    public String generateYearlyRainfallLineChartHtml() {
        List<RainfallRecord> allData = rainfallDataService.getAllData();
        if (allData.isEmpty()) return "<div>No data available.</div>";

        int minYear = allData.stream().mapToInt(RainfallRecord::getYear).min().orElse(1901);
        int maxYear = allData.stream().mapToInt(RainfallRecord::getYear).max().orElse(2021);
        double maxRain = allData.stream().mapToDouble(RainfallRecord::getTotal).max().orElse(2000.0);
        double minRain = allData.stream().mapToDouble(RainfallRecord::getTotal).min().orElse(0.0);

        int chartWidth = 700;
        int chartHeight = 280;
        int leftPad = 60;
        int rightPad = 30;
        int topPad = 30;
        int bottomPad = 50;
        int n = allData.size();
        double xStep = (double)(chartWidth - leftPad - rightPad) / (n - 1);

        StringBuilder html = new StringBuilder();
        html.append("<div style='padding: 20px; background: #f8f9fa; border-radius: 8px; margin: 20px 0;'>");
        html.append("<h4>📈 Yearly Rainfall (" + minYear + "–" + maxYear + ")</h4>");
        html.append("<div style='position: relative; width: 100%; height: 400px; background: white; border: 1px solid #ddd; padding: 20px; box-sizing: border-box;'>");
        html.append("<svg width='100%' height='350' viewBox='0 0 " + chartWidth + " " + (chartHeight + topPad + bottomPad) + "' style='overflow: visible;'>");

        // Draw grid lines and y-axis labels
        int gridLines = 5;
        for (int i = 0; i <= gridLines; i++) {
            double yVal = minRain + (maxRain - minRain) * (gridLines - i) / gridLines;
            int y = (int)(topPad + ((maxRain - yVal) / (maxRain - minRain)) * chartHeight);
            html.append(String.format("<line x1='%d' y1='%d' x2='%d' y2='%d' stroke='#ecf0f1' stroke-width='1'/>",
                leftPad, y, chartWidth - rightPad, y));
            html.append(String.format("<text x='%d' y='%d' text-anchor='end' font-size='10' fill='#7f8c8d'>%.0f</text>",
                leftPad - 5, y + 4, yVal));
        }

        // Draw x-axis labels (every 10th year)
        int labelStep = 10;
        for (int i = 0; i < n; i++) {
            int year = allData.get(i).getYear();
            if ((year - minYear) % labelStep == 0 || i == n - 1) {
                double x = leftPad + i * xStep;
                html.append(String.format("<text x='%.1f' y='%d' text-anchor='middle' font-size='10' fill='#7f8c8d'>%d</text>",
                    x, chartHeight + topPad + 20, year));
            }
        }

        // Draw axes
        html.append(String.format("<line x1='%d' y1='%d' x2='%d' y2='%d' stroke='#2c3e50' stroke-width='2'/>",
            leftPad, topPad, leftPad, chartHeight + topPad));
        html.append(String.format("<line x1='%d' y1='%d' x2='%d' y2='%d' stroke='#2c3e50' stroke-width='2'/>",
            leftPad, chartHeight + topPad, chartWidth - rightPad, chartHeight + topPad));

        // Draw line path
        html.append("<polyline fill='none' stroke='#3498db' stroke-width='2' points='");
        for (int i = 0; i < n; i++) {
            double x = leftPad + i * xStep;
            double y = topPad + ((maxRain - allData.get(i).getTotal()) / (maxRain - minRain)) * chartHeight;
            html.append(String.format("%.1f,%.1f ", x, y));
        }
        html.append("'/>");

        // Optionally, draw dots for each year (for clarity, only every 5th year)
        for (int i = 0; i < n; i += 5) {
            double x = leftPad + i * xStep;
            double y = topPad + ((maxRain - allData.get(i).getTotal()) / (maxRain - minRain)) * chartHeight;
            html.append(String.format("<circle cx='%.1f' cy='%.1f' r='2.5' fill='#e74c3c' stroke='#fff' stroke-width='1'/>", x, y));
        }

        // Axis labels
        html.append("<text x='20' y='" + (topPad + chartHeight/2) + "' text-anchor='middle' font-size='12' font-weight='bold' fill='#2c3e50' transform='rotate(-90, 20, " + (topPad + chartHeight/2) + ")'>Rainfall (mm)</text>");
        html.append("<text x='" + (chartWidth/2) + "' y='" + (chartHeight + topPad + 40) + "' text-anchor='middle' font-size='12' font-weight='bold' fill='#2c3e50'>Year</text>");

        html.append("</svg>");
        html.append("</div>");
        html.append("</div>");
        return html.toString();
    }
    /**
     * Generate a decade-wise rainfall comparison chart with a user-defined offset.
     * For example, offset=5 means intervals are 1905-1914, 1915-1924, etc.
     * @param offset integer between 0 and 9
     * @return HTML string with SVG chart
     */
    public String generateDecadeComparisonChartHtmlWithOffset(int offset) {
        List<RainfallRecord> allData = rainfallDataService.getAllData();
        if (offset < 0 || offset > 9) offset = 0;

        // Find min and max year
        int minYear = allData.stream().mapToInt(RainfallRecord::getYear).min().orElse(1901);
        int maxYear = allData.stream().mapToInt(RainfallRecord::getYear).max().orElse(2021);

        // Calculate intervals
        List<int[]> intervals = new ArrayList<>();
        int start = minYear - ((minYear - offset) % 10);
        if (start < minYear) start += 10;
        for (int s = start; s <= maxYear; s += 10) {
            int e = s + 9;
            intervals.add(new int[]{s, Math.min(e, maxYear)});
        }

        // Group data by intervals
        Map<String, List<Double>> intervalData = new LinkedHashMap<>();
        for (int[] interval : intervals) {
            int s = interval[0], e = interval[1];
            String label = s + "-" + e;
            List<Double> values = allData.stream()
                    .filter(r -> r.getYear() >= s && r.getYear() <= e)
                    .map(RainfallRecord::getTotal)
                    .collect(Collectors.toList());
            if (!values.isEmpty()) intervalData.put(label, values);
        }

        StringBuilder html = new StringBuilder();
        html.append("<div style='padding: 20px; background: #f8f9fa; border-radius: 8px; margin: 20px 0;'>");
        html.append("<h4>📊 Decade-wise Rainfall Comparison (Offset: ").append(offset).append(")</h4>");
        html.append("<div style='position: relative; width: 100%; height: 400px; background: white; border: 1px solid #ddd; padding: 20px; box-sizing: border-box;'>");
        html.append("<svg width='100%' height='350' style='overflow: visible;'>");

        int chartWidth = 600;
        int chartHeight = 280;
        int barWidth = 35;
        int spacing = 20;

        double maxValue = intervalData.values().stream()
                .mapToDouble(values -> values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0))
                .max().orElse(2000.0);

        // Draw grid lines
        for (int i = 0; i <= 5; i++) {
            int yValue = (int) (maxValue * i / 5);
            int yPos = (int) (chartHeight - (chartHeight * i / 5) + 20);
            html.append(String.format("<line x1='50' y1='%d' x2='%d' y2='%d' stroke='#ecf0f1' stroke-width='1'/>",
                    yPos, chartWidth - 50, yPos));
            html.append(String.format("<text x='45' y='%d' text-anchor='end' font-size='10' fill='#7f8c8d'>%d</text>",
                    yPos + 3, yValue));
        }

        int x = 80;
        int index = 0;
        String[] colors = {"#3498db", "#e74c3c", "#2ecc71", "#f39c12", "#9b59b6", "#1abc9c", "#34495e", "#e67e22", "#95a5a6", "#f1c40f", "#8e44ad", "#27ae60", "#2980b9"};

        for (Map.Entry<String, List<Double>> entry : intervalData.entrySet()) {
            String label = entry.getKey();
            double average = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            int barHeight = (int) ((average / maxValue) * chartHeight);
            int barY = chartHeight - barHeight + 20;
            String color = colors[index % colors.length];
            html.append(String.format("<rect x='%d' y='%d' width='%d' height='%d' fill='%s' stroke='#2c3e50' stroke-width='1' rx='2'/>",
                    x, barY, barWidth, barHeight, color));
            html.append(String.format("<text x='%d' y='%d' text-anchor='middle' font-size='11' font-weight='bold' fill='#2c3e50'>%.0f</text>",
                    x + barWidth/2, barY - 5, average));
            html.append(String.format("<text x='%d' y='%d' text-anchor='middle' font-size='10' fill='#7f8c8d' transform='rotate(-45, %d, %d)'>%s</text>",
                    x + barWidth/2, chartHeight + 45, x + barWidth/2, chartHeight + 45, label));
            x += barWidth + spacing;
            index++;
        }

        html.append(String.format("<line x1='50' y1='20' x2='50' y2='%d' stroke='#2c3e50' stroke-width='2'/>", chartHeight + 20));
        html.append(String.format("<line x1='50' y1='%d' x2='%d' y2='%d' stroke='#2c3e50' stroke-width='2'/>",
                chartHeight + 20, chartWidth - 50, chartHeight + 20));
        html.append("<text x='25' y='160' text-anchor='middle' font-size='12' font-weight='bold' fill='#2c3e50' transform='rotate(-90, 25, 160)'>Rainfall (mm)</text>");
        html.append("<text x='325' y='380' text-anchor='middle' font-size='12' font-weight='bold' fill='#2c3e50'>Interval</text>");
        html.append("</svg>");
        html.append("</div>");

        html.append("<div style='margin-top: 15px; font-size: 14px; color: #7f8c8d;'>");
        double maxAvg = intervalData.values().stream()
                .mapToDouble(values -> values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0))
                .max().orElse(0.0);
        double minAvg = intervalData.values().stream()
                .mapToDouble(values -> values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0))
                .min().orElse(0.0);
        html.append(String.format("<strong>Summary:</strong> Highest interval: %.0f mm | Lowest interval: %.0f mm | Range: %.0f mm",
                maxAvg, minAvg, maxAvg - minAvg));
        html.append("</div>");
        html.append("</div>");
        return html.toString();
    }
    
    public String generateMonthlyAverageChartHtml() {
        StringBuilder html = new StringBuilder();
        html.append("<div class='chart-container' style='margin: 20px 0;'>");
        html.append("<h4>📊 Monthly Rainfall Averages (1901-2021)</h4>");
        html.append("<canvas id='monthlyChart' width='600' height='400'></canvas>");
        html.append("</div>");
        
        // Generate the chart data
        html.append("<script>");
        html.append("if (typeof Chart !== 'undefined') {");
        html.append("const ctx2 = document.getElementById('monthlyChart');");
        html.append("if (ctx2) {");
        html.append("const chart2 = new Chart(ctx2.getContext('2d'), {");
        html.append("type: 'bar',");
        html.append("data: {");
        html.append("labels: ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'],");
        html.append("datasets: [{");
        html.append("label: 'Average Rainfall (mm)',");
        html.append("data: [");
        
        // Monthly averages
        for (int month = 1; month <= 12; month++) {
            if (month > 1) html.append(",");
            html.append(String.format("%.1f", rainfallDataService.getAverageRainfallForMonth(month)));
        }
        html.append("],");
        html.append("backgroundColor: [");
        html.append("'rgba(255, 99, 132, 0.6)','rgba(54, 162, 235, 0.6)','rgba(255, 205, 86, 0.6)',");
        html.append("'rgba(75, 192, 192, 0.6)','rgba(153, 102, 255, 0.6)','rgba(255, 159, 64, 0.6)',");
        html.append("'rgba(199, 199, 199, 0.6)','rgba(83, 102, 255, 0.6)','rgba(255, 99, 255, 0.6)',");
        html.append("'rgba(99, 255, 132, 0.6)','rgba(255, 199, 64, 0.6)','rgba(132, 99, 255, 0.6)'");
        html.append("],");
        html.append("borderColor: [");
        html.append("'rgba(255, 99, 132, 1)','rgba(54, 162, 235, 1)','rgba(255, 205, 86, 1)',");
        html.append("'rgba(75, 192, 192, 1)','rgba(153, 102, 255, 1)','rgba(255, 159, 64, 1)',");
        html.append("'rgba(199, 199, 199, 1)','rgba(83, 102, 255, 1)','rgba(255, 99, 255, 1)',");
        html.append("'rgba(99, 255, 132, 1)','rgba(255, 199, 64, 1)','rgba(132, 99, 255, 1)'");
        html.append("],");
        html.append("borderWidth: 1");
        html.append("}]");
        html.append("},");
        html.append("options: {");
        html.append("responsive: true,");
        html.append("plugins: { legend: { display: true } },");
        html.append("scales: {");
        html.append("y: { beginAtZero: true, title: { display: true, text: 'Rainfall (mm)' } },");
        html.append("x: { title: { display: true, text: 'Month' } }");
        html.append("}");
        html.append("}");
        html.append("});");
        html.append("}");
        html.append("} else { console.error('Chart.js not loaded'); }");
        html.append("</script>");
        
        return html.toString();
    }
    
    public String generateDecadeComparisonChartHtml() {
        List<RainfallRecord> allData = rainfallDataService.getAllData();
        
        // Group by decades
        Map<String, List<Double>> decadeData = new TreeMap<>();
        for (RainfallRecord record : allData) {
            int decade = (record.getYear() / 10) * 10;
            String decadeLabel = decade + "s";
            decadeData.computeIfAbsent(decadeLabel, k -> new ArrayList<>()).add(record.getTotal());
        }
        
        StringBuilder html = new StringBuilder();
        
        // Generate SVG chart (no JavaScript required)
        html.append("<div style='padding: 20px; background: #f8f9fa; border-radius: 8px; margin: 20px 0;'>");
        html.append("<h4>📊 Decade-wise Rainfall Comparison</h4>");
        html.append("<div style='position: relative; width: 100%; height: 400px; background: white; border: 1px solid #ddd; padding: 20px; box-sizing: border-box;'>");
        
        // SVG container
        html.append("<svg width='100%' height='350' style='overflow: visible;'>");
        
        // Calculate chart dimensions
        int chartWidth = 600;
        int chartHeight = 280;
        int barWidth = 35;
        int spacing = 20;
        
        // Calculate max value for scaling
        double maxValue = decadeData.values().stream()
                .mapToDouble(values -> values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0))
                .max().orElse(2000.0);
        
        // Draw grid lines
        for (int i = 0; i <= 5; i++) {
            int yValue = (int) (maxValue * i / 5);
            int yPos = (int) (chartHeight - (chartHeight * i / 5) + 20);
            html.append(String.format("<line x1='50' y1='%d' x2='%d' y2='%d' stroke='#ecf0f1' stroke-width='1'/>", 
                    yPos, chartWidth - 50, yPos));
            html.append(String.format("<text x='45' y='%d' text-anchor='end' font-size='10' fill='#7f8c8d'>%d</text>", 
                    yPos + 3, yValue));
        }
        
        // Draw bars
        int x = 80;
        int index = 0;
        String[] colors = {"#3498db", "#e74c3c", "#2ecc71", "#f39c12", "#9b59b6", "#1abc9c", "#34495e", "#e67e22", "#95a5a6", "#f1c40f", "#8e44ad", "#27ae60", "#2980b9"};
        
        for (Map.Entry<String, List<Double>> entry : decadeData.entrySet()) {
            String decade = entry.getKey();
            double average = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            int barHeight = (int) ((average / maxValue) * chartHeight);
            int barY = chartHeight - barHeight + 20;
            
            String color = colors[index % colors.length];
            
            // Draw bar
            html.append(String.format("<rect x='%d' y='%d' width='%d' height='%d' fill='%s' stroke='#2c3e50' stroke-width='1' rx='2'/>", 
                    x, barY, barWidth, barHeight, color));
            
            // Draw value label on top of bar
            html.append(String.format("<text x='%d' y='%d' text-anchor='middle' font-size='11' font-weight='bold' fill='#2c3e50'>%.0f</text>", 
                    x + barWidth/2, barY - 5, average));
            
            // Draw decade label below bar
            html.append(String.format("<text x='%d' y='%d' text-anchor='middle' font-size='10' fill='#7f8c8d' transform='rotate(-45, %d, %d)'>%s</text>", 
                    x + barWidth/2, chartHeight + 45, x + barWidth/2, chartHeight + 45, decade));
            
            x += barWidth + spacing;
            index++;
        }
        
        // Draw axes
        html.append(String.format("<line x1='50' y1='20' x2='50' y2='%d' stroke='#2c3e50' stroke-width='2'/>", chartHeight + 20));
        html.append(String.format("<line x1='50' y1='%d' x2='%d' y2='%d' stroke='#2c3e50' stroke-width='2'/>", 
                chartHeight + 20, chartWidth - 50, chartHeight + 20));
        
        // Add axis labels
        html.append("<text x='25' y='160' text-anchor='middle' font-size='12' font-weight='bold' fill='#2c3e50' transform='rotate(-90, 25, 160)'>Rainfall (mm)</text>");
        html.append("<text x='325' y='380' text-anchor='middle' font-size='12' font-weight='bold' fill='#2c3e50'>Decade</text>");
        
        html.append("</svg>");
        html.append("</div>");
        
        // Add summary statistics
        html.append("<div style='margin-top: 15px; font-size: 14px; color: #7f8c8d;'>");
        double maxAvg = decadeData.values().stream()
                .mapToDouble(values -> values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0))
                .max().orElse(0.0);
        double minAvg = decadeData.values().stream()
                .mapToDouble(values -> values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0))
                .min().orElse(0.0);
        html.append(String.format("<strong>Summary:</strong> Highest decade: %.0f mm | Lowest decade: %.0f mm | Range: %.0f mm", 
                maxAvg, minAvg, maxAvg - minAvg));
        html.append("</div>");
        html.append("</div>");
        
        return html.toString();
    }
    
    /**
     * Generate qualitative observations about rainfall trends based on yearly data.
     * @return HTML string with trend analysis
     */
    public String generateRainfallTrendAnalysisHtml() {
        List<RainfallRecord> allData = rainfallDataService.getAllData();
        if (allData.isEmpty()) return "<div>No data available for trend analysis.</div>";
        
        // Calculate various trend metrics
        List<Double> yearlyTotals = allData.stream().map(RainfallRecord::getTotal).collect(Collectors.toList());
        List<Integer> years = allData.stream().map(RainfallRecord::getYear).collect(Collectors.toList());
        
        double avgRainfall = yearlyTotals.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double maxRainfall = yearlyTotals.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        double minRainfall = yearlyTotals.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        
        // Find years with extreme values
        int maxYear = 0, minYear = 0;
        for (int i = 0; i < allData.size(); i++) {
            if (allData.get(i).getTotal() == maxRainfall) maxYear = allData.get(i).getYear();
            if (allData.get(i).getTotal() == minRainfall) minYear = allData.get(i).getYear();
        }
        
        // Calculate trend line
        double[] trendLine = calculateTrendLine(years, yearlyTotals);
        
        // Calculate variability
        double variance = yearlyTotals.stream()
            .mapToDouble(x -> Math.pow(x - avgRainfall, 2))
            .average().orElse(0.0);
        double stdDev = Math.sqrt(variance);
        double coeffVariation = (stdDev / avgRainfall) * 100;
        
        // Decade analysis
        Map<String, Double> decadeAverages = new LinkedHashMap<>();
        Map<String, List<Double>> decadeData = new TreeMap<>();
        for (RainfallRecord record : allData) {
            int decade = (record.getYear() / 10) * 10;
            String decadeLabel = decade + "s";
            decadeData.computeIfAbsent(decadeLabel, k -> new ArrayList<>()).add(record.getTotal());
        }
        for (Map.Entry<String, List<Double>> entry : decadeData.entrySet()) {
            double avg = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            decadeAverages.put(entry.getKey(), avg);
        }
        
        StringBuilder html = new StringBuilder();
        html.append("<div style='padding: 20px; background: #f8f9fa; border-radius: 8px; margin: 20px 0;'>");
        html.append("<h4>🔍 Rainfall Trend Analysis & Qualitative Observations</h4>");
        
        html.append("<div class='row'>");
        html.append("<div class='col-md-6'>");
        html.append("<div style='background: white; padding: 15px; border-radius: 6px; border-left: 4px solid #3498db;'>");
        html.append("<h6>📊 Statistical Overview</h6>");
        html.append("<ul style='list-style: none; padding: 0;'>");
        html.append(String.format("<li>• <strong>Average Annual Rainfall:</strong> %.1f mm</li>", avgRainfall));
        html.append(String.format("<li>• <strong>Range:</strong> %.1f mm (%.1f - %.1f mm)</li>", maxRainfall - minRainfall, minRainfall, maxRainfall));
        html.append(String.format("<li>• <strong>Wettest Year:</strong> %d (%.1f mm)</li>", maxYear, maxRainfall));
        html.append(String.format("<li>• <strong>Driest Year:</strong> %d (%.1f mm)</li>", minYear, minRainfall));
        html.append(String.format("<li>• <strong>Variability:</strong> %.1f%% (Coefficient of Variation)</li>", coeffVariation));
        html.append("</ul>");
        html.append("</div>");
        html.append("</div>");
        
        html.append("<div class='col-md-6'>");
        html.append("<div style='background: white; padding: 15px; border-radius: 6px; border-left: 4px solid #e67e22;'>");
        html.append("<h6>📈 Trend Analysis</h6>");
        
        if (trendLine != null) {
            double slope = trendLine[0];
            String trendDirection;
            String trendIcon;
            String trendColor;
            
            if (slope > 2.0) {
                trendDirection = "Strong Increasing Trend";
                trendIcon = "📈";
                trendColor = "#27ae60";
            } else if (slope > 0.5) {
                trendDirection = "Moderate Increasing Trend";
                trendIcon = "📊";
                trendColor = "#2ecc71";
            } else if (slope > -0.5) {
                trendDirection = "Stable Pattern";
                trendIcon = "➡️";
                trendColor = "#f39c12";
            } else if (slope > -2.0) {
                trendDirection = "Moderate Decreasing Trend";
                trendIcon = "📉";
                trendColor = "#e67e22";
            } else {
                trendDirection = "Strong Decreasing Trend";
                trendIcon = "⬇️";
                trendColor = "#e74c3c";
            }
            
            html.append("<ul style='list-style: none; padding: 0;'>");
            html.append(String.format("<li style='color: %s;'>%s <strong>%s</strong></li>", trendColor, trendIcon, trendDirection));
            html.append(String.format("<li>• <strong>Rate of Change:</strong> %.2f mm/year</li>", slope));
            html.append(String.format("<li>• <strong>Total Change (120 years):</strong> %.1f mm</li>", slope * 120));
            html.append("</ul>");
        }
        html.append("</div>");
        html.append("</div>");
        html.append("</div>");
        
        // Qualitative observations
        html.append("<div style='background: white; padding: 15px; border-radius: 6px; margin-top: 15px; border-left: 4px solid #9b59b6;'>");
        html.append("<h6>🔍 Key Observations</h6>");
        
        // Climate variability assessment
        String variabilityLevel;
        if (coeffVariation > 30) {
            variabilityLevel = "High variability suggests significant climate variability and potential climate change impacts.";
        } else if (coeffVariation > 20) {
            variabilityLevel = "Moderate variability indicates normal monsoon variations typical for the region.";
        } else {
            variabilityLevel = "Low variability suggests relatively stable rainfall patterns.";
        }
        
        // Extreme events analysis
        long extremeHighYears = yearlyTotals.stream().mapToLong(x -> x > (avgRainfall + 1.5 * stdDev) ? 1 : 0).sum();
        long extremeLowYears = yearlyTotals.stream().mapToLong(x -> x < (avgRainfall - 1.5 * stdDev) ? 1 : 0).sum();
        
        html.append("<ul>");
        html.append("<li><strong>Climate Variability:</strong> ").append(variabilityLevel).append("</li>");
        html.append(String.format("<li><strong>Extreme Events:</strong> %d years with exceptionally high rainfall and %d years with exceptionally low rainfall (beyond 1.5 standard deviations).</li>", extremeHighYears, extremeLowYears));
        
        // Decade comparison
        String bestDecade = decadeAverages.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey).orElse("Unknown");
        String worstDecade = decadeAverages.entrySet().stream()
            .min(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey).orElse("Unknown");
            
        html.append(String.format("<li><strong>Decadal Patterns:</strong> The %s was the wettest decade while the %s was the driest.</li>", bestDecade, worstDecade));
        
        // Monsoon strength assessment
        if (avgRainfall > 1400) {
            html.append("<li><strong>Monsoon Strength:</strong> Chennai receives robust monsoon rainfall, characteristic of its tropical wet and dry climate.</li>");
        } else if (avgRainfall > 1000) {
            html.append("<li><strong>Monsoon Strength:</strong> Chennai experiences moderate monsoon rainfall, typical for its coastal location.</li>");
        } else {
            html.append("<li><strong>Monsoon Strength:</strong> Chennai shows lower rainfall patterns, possibly influenced by urban heat island effects or climate shifts.</li>");
        }
        
        html.append("</ul>");
        html.append("</div>");
        
        html.append("</div>");
        return html.toString();
    }
    
    /**
     * Generate a monthly trend line chart for a specific month across all years.
     * @param month Month number (1-12) where 1=Jan, 2=Feb, etc.
     * @return HTML string with SVG chart
     */
    public String generateMonthlyTrendLineChartHtml(int month) {
        List<RainfallRecord> allData = rainfallDataService.getAllData();
        if (allData.isEmpty()) return "<div>No data available.</div>";
        
        // Validate month parameter
        if (month < 1 || month > 12) {
            return "<div class='alert alert-danger'>Invalid month. Please specify a month between 1 (Jan) and 12 (Dec).</div>";
        }
        
        String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        String monthName = monthNames[month - 1];
        
        // Extract data for the specific month
        List<Double> monthlyValues = new ArrayList<>();
        List<Integer> years = new ArrayList<>();
        
        for (RainfallRecord record : allData) {
            double value = getMonthValue(record, month);
            monthlyValues.add(value);
            years.add(record.getYear());
        }
        
        if (monthlyValues.isEmpty()) {
            return "<div>No data available for " + monthName + ".</div>";
        }
        
        int minYear = years.stream().mapToInt(Integer::intValue).min().orElse(1901);
        int maxYear = years.stream().mapToInt(Integer::intValue).max().orElse(2021);
        double maxRain = monthlyValues.stream().mapToDouble(Double::doubleValue).max().orElse(500.0);
        double minRain = monthlyValues.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        
        // Add some padding to the max value for better visualization
        maxRain = maxRain * 1.1;
        
        int chartWidth = 700;
        int chartHeight = 280;
        int leftPad = 60;
        int rightPad = 30;
        int topPad = 30;
        int bottomPad = 50;
        int n = monthlyValues.size();
        double xStep = (double)(chartWidth - leftPad - rightPad) / (n - 1);
        
        StringBuilder html = new StringBuilder();
        html.append("<div style='padding: 20px; background: #f8f9fa; border-radius: 8px; margin: 20px 0;'>");
        html.append("<h4>📈 ").append(monthName).append(" Rainfall Trend (").append(minYear).append("–").append(maxYear).append(")</h4>");
        html.append("<div style='position: relative; width: 100%; height: 400px; background: white; border: 1px solid #ddd; padding: 20px; box-sizing: border-box;'>");
        html.append("<svg width='100%' height='350' viewBox='0 0 " + chartWidth + " " + (chartHeight + topPad + bottomPad) + "' style='overflow: visible;'>");
        
        // Draw grid lines and y-axis labels
        int gridLines = 5;
        for (int i = 0; i <= gridLines; i++) {
            double yVal = minRain + (maxRain - minRain) * (gridLines - i) / gridLines;
            int y = (int)(topPad + ((maxRain - yVal) / (maxRain - minRain)) * chartHeight);
            html.append(String.format("<line x1='%d' y1='%d' x2='%d' y2='%d' stroke='#ecf0f1' stroke-width='1'/>",
                leftPad, y, chartWidth - rightPad, y));
            html.append(String.format("<text x='%d' y='%d' text-anchor='end' font-size='10' fill='#7f8c8d'>%.0f</text>",
                leftPad - 5, y + 4, yVal));
        }
        
        // Draw x-axis labels (every 10th year)
        int labelStep = 10;
        for (int i = 0; i < n; i++) {
            int year = years.get(i);
            if ((year - minYear) % labelStep == 0 || i == n - 1) {
                double x = leftPad + i * xStep;
                html.append(String.format("<text x='%.1f' y='%d' text-anchor='middle' font-size='10' fill='#7f8c8d'>%d</text>",
                    x, chartHeight + topPad + 20, year));
            }
        }
        
        // Draw axes
        html.append(String.format("<line x1='%d' y1='%d' x2='%d' y2='%d' stroke='#2c3e50' stroke-width='2'/>",
            leftPad, topPad, leftPad, chartHeight + topPad));
        html.append(String.format("<line x1='%d' y1='%d' x2='%d' y2='%d' stroke='#2c3e50' stroke-width='2'/>",
            leftPad, chartHeight + topPad, chartWidth - rightPad, chartHeight + topPad));
        
        // Draw line path
        html.append("<polyline fill='none' stroke='#e67e22' stroke-width='2' points='");
        for (int i = 0; i < n; i++) {
            double x = leftPad + i * xStep;
            double y = topPad + ((maxRain - monthlyValues.get(i)) / (maxRain - minRain)) * chartHeight;
            html.append(String.format("%.1f,%.1f ", x, y));
        }
        html.append("'/>");
        
        // Draw dots for each data point (every 5th year for clarity)
        for (int i = 0; i < n; i += 5) {
            double x = leftPad + i * xStep;
            double y = topPad + ((maxRain - monthlyValues.get(i)) / (maxRain - minRain)) * chartHeight;
            html.append(String.format("<circle cx='%.1f' cy='%.1f' r='2.5' fill='#c0392b' stroke='#fff' stroke-width='1'/>", x, y));
        }
        
        // Calculate and display trend line (simple linear regression)
        double[] trendLine = calculateTrendLine(years, monthlyValues);
        if (trendLine != null) {
            double slope = trendLine[0];
            double intercept = trendLine[1];
            
            // Draw trend line
            double startX = leftPad;
            double endX = leftPad + (n - 1) * xStep;
            double startY = topPad + ((maxRain - (slope * minYear + intercept)) / (maxRain - minRain)) * chartHeight;
            double endY = topPad + ((maxRain - (slope * maxYear + intercept)) / (maxRain - minRain)) * chartHeight;
            
            html.append(String.format("<line x1='%.1f' y1='%.1f' x2='%.1f' y2='%.1f' stroke='#27ae60' stroke-width='2' stroke-dasharray='5,5' opacity='0.8'/>",
                startX, startY, endX, endY));
        }
        
        // Axis labels
        html.append("<text x='20' y='" + (topPad + chartHeight/2) + "' text-anchor='middle' font-size='12' font-weight='bold' fill='#2c3e50' transform='rotate(-90, 20, " + (topPad + chartHeight/2) + ")'>Rainfall (mm)</text>");
        html.append("<text x='" + (chartWidth/2) + "' y='" + (chartHeight + topPad + 40) + "' text-anchor='middle' font-size='12' font-weight='bold' fill='#2c3e50'>Year</text>");
        
        html.append("</svg>");
        html.append("</div>");
        
        // Add summary statistics
        double avgRainfall = monthlyValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double maxMonthRain = monthlyValues.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        double minMonthRain = monthlyValues.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        
        html.append("<div style='margin-top: 15px; font-size: 14px; color: #7f8c8d;'>");
        html.append(String.format("<strong>%s Summary:</strong> Average: %.1f mm | Highest: %.1f mm | Lowest: %.1f mm | Range: %.1f mm",
            monthName, avgRainfall, maxMonthRain, minMonthRain, maxMonthRain - minMonthRain));
        
        if (trendLine != null) {
            double slope = trendLine[0];
            String trendDirection = slope > 0.1 ? "increasing" : slope < -0.1 ? "decreasing" : "stable";
            html.append(String.format(" | Trend: %s (%.2f mm/year)", trendDirection, slope));
        }
        
        html.append("</div>");
        html.append("</div>");
        return html.toString();
    }
    
    /**
     * Helper method to get rainfall value for a specific month from a record.
     */
    private double getMonthValue(RainfallRecord record, int month) {
        switch (month) {
            case 1: return record.getJan();
            case 2: return record.getFeb();
            case 3: return record.getMar();
            case 4: return record.getApril();
            case 5: return record.getMay();
            case 6: return record.getJune();
            case 7: return record.getJuly();
            case 8: return record.getAug();
            case 9: return record.getSept();
            case 10: return record.getOct();
            case 11: return record.getNov();
            case 12: return record.getDec();
            default: return 0.0;
        }
    }
    
    /**
     * Calculate simple linear regression trend line.
     * Returns [slope, intercept] or null if calculation fails.
     */
    private double[] calculateTrendLine(List<Integer> years, List<Double> values) {
        if (years.size() != values.size() || years.size() < 2) return null;
        
        int n = years.size();
        double sumX = years.stream().mapToDouble(Integer::doubleValue).sum();
        double sumY = values.stream().mapToDouble(Double::doubleValue).sum();
        double sumXY = 0.0;
        double sumXX = 0.0;
        
        for (int i = 0; i < n; i++) {
            double x = years.get(i);
            double y = values.get(i);
            sumXY += x * y;
            sumXX += x * x;
        }
        
        double slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;
        
        return new double[]{slope, intercept};
    }

    public String generateRainfallStatisticsHtml() {
        Map<String, Object> stats = rainfallDataService.getBasicStatistics();
        
        StringBuilder html = new StringBuilder();
        html.append("<div class='card'>");
        html.append("<div class='card-header'><h5>📊 Rainfall Statistics Summary</h5></div>");
        html.append("<div class='card-body'>");
        
        html.append("<div class='row'>");
        html.append("<div class='col-md-6'>");
        html.append("<h6>General Statistics:</h6>");
        html.append("<ul class='list-group list-group-flush'>");
        html.append("<li class='list-group-item'>Total Records: ").append(stats.get("totalRecords")).append("</li>");
        html.append("<li class='list-group-item'>Year Range: ").append(stats.get("yearRange")).append("</li>");
        html.append("<li class='list-group-item'>Average Annual Rainfall: ").append(stats.get("averageAnnualRainfall")).append(" mm</li>");
        html.append("<li class='list-group-item'>Highest: ").append(stats.get("highestRainfallYear")).append("</li>");
        html.append("<li class='list-group-item'>Lowest: ").append(stats.get("lowestRainfallYear")).append("</li>");
        html.append("</ul>");
        html.append("</div>");
        
        html.append("<div class='col-md-6'>");
        html.append("<h6>Monthly Averages (mm):</h6>");
        html.append("<ul class='list-group list-group-flush'>");
        
        @SuppressWarnings("unchecked")
        Map<String, Double> monthlyAverages = (Map<String, Double>) stats.get("monthlyAverages");
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        for (String month : months) {
            html.append("<li class='list-group-item'>").append(month).append(": ").append(monthlyAverages.get(month)).append("</li>");
        }
        
        html.append("</ul>");
        html.append("</div>");
        html.append("</div>");
        
        html.append("</div>");
        html.append("</div>");
        
        return html.toString();
    }
}
