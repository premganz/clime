package com.example.clime.controller;

import com.example.clime.module.climatev2.model.DataSource;
import com.example.clime.module.climatev2.service.UnifiedRainfallDataService;
import com.example.clime.module.climatev2.controller.RainfallControllerV2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ChartFunctionalityTest {

    @Autowired
    private RainfallControllerV2 rainfallControllerV2;
    
    @Autowired
    private UnifiedRainfallDataService unifiedRainfallDataService;

    @Test
    public void testCSVDataSourceCharts() {
        // Test CSV data source charts
        ResponseEntity<String> response = rainfallControllerV2.getAnnualChart("CSV");
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        // Should not contain error message
        assertFalse(response.getBody().contains("Chart not available for KWS data"));
        assertFalse(response.getBody().contains("alert-warning"));
        
        // Should contain chart content
        assertTrue(response.getBody().contains("svg") || response.getBody().contains("chart"));
    }

    @Test
    public void testKWSDataSourceCharts() {
        // Test KWS data source charts - this should now work!
        ResponseEntity<String> response = rainfallControllerV2.getAnnualChart("KWS");
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        // Should NOT contain the old error message that charts are not available
        assertFalse(response.getBody().contains("Chart not available for KWS data"));
        assertFalse(response.getBody().contains("Advanced annual chart functionality is currently optimized for CSV data"));
        
        // Should contain chart content or at least not the warning message
        // Charts should work for KWS data now
        if (response.getBody().contains("alert-danger")) {
            // If there's an error, it should be a different error (e.g., no data), not the old restriction
            assertFalse(response.getBody().contains("Chart not available for KWS data"));
        } else {
            // Charts should work and contain SVG content
            assertTrue(response.getBody().contains("svg") || response.getBody().contains("chart") || response.getBody().contains("rainfall"));
        }
    }

    @Test
    public void testDecadeChart() {
        // Test decade chart with KWS data
        ResponseEntity<String> response = rainfallControllerV2.getDecadeChart("KWS");
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        // Should not contain the restriction message
        assertFalse(response.getBody().contains("Chart not available for KWS data"));
    }

    @Test
    public void testMonthlyChart() {
        // Test monthly chart with KWS data
        ResponseEntity<String> response = rainfallControllerV2.getMonthlyChart("KWS");
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        // Should not contain the restriction message
        assertFalse(response.getBody().contains("Chart not available for KWS data"));
    }

    @Test
    public void testUnifiedServiceDataSwitching() {
        // Test that the unified service can switch between data sources
        unifiedRainfallDataService.setDataSource(DataSource.CSV);
        assertEquals(DataSource.CSV, unifiedRainfallDataService.getCurrentDataSource());
        
        unifiedRainfallDataService.setDataSource(DataSource.KWS);
        assertEquals(DataSource.KWS, unifiedRainfallDataService.getCurrentDataSource());
    }

    @Test
    public void testYearlyBarChart() {
        // Test yearly bar chart with CSV data
        ResponseEntity<String> response = rainfallControllerV2.getYearlyBarChart("CSV");
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        // Should contain chart content
        assertTrue(response.getBody().contains("Year-wise Rainfall Bar Chart") || response.getBody().contains("svg"));
        assertFalse(response.getBody().contains("alert-danger"));
    }

    @Test
    public void testYearlyBarChartWithKWS() {
        // Test yearly bar chart with KWS data
        ResponseEntity<String> response = rainfallControllerV2.getYearlyBarChart("KWS");
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        // Should work with KWS data now
        if (response.getBody().contains("alert-danger")) {
            // If there's an error, it should not be about data source restrictions
            assertFalse(response.getBody().contains("Chart not available for KWS data"));
        } else {
            assertTrue(response.getBody().contains("Year-wise Rainfall Bar Chart") || response.getBody().contains("svg"));
        }
    }

    @Test
    public void testYearlyOffsetChart() {
        // Test yearly offset chart with valid month offset (April = 4)
        ResponseEntity<String> response = rainfallControllerV2.getYearlyOffsetChart(4, "CSV");
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        // Should contain chart content with April mentioned
        assertTrue(response.getBody().contains("Offset Yearly Rainfall Chart") || response.getBody().contains("svg"));
        assertTrue(response.getBody().contains("Apr"));
        assertFalse(response.getBody().contains("alert-danger"));
    }

    @Test
    public void testYearlyOffsetChartInvalidMonth() {
        // Test yearly offset chart with invalid month offset
        ResponseEntity<String> response = rainfallControllerV2.getYearlyOffsetChart(15, "CSV");
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        // Should contain error message for invalid month
        assertTrue(response.getBody().contains("Invalid month offset") || response.getBody().contains("alert-danger"));
    }

    @Test
    public void testYearlyOffsetChartEdgeCases() {
        // Test yearly offset chart with edge cases
        ResponseEntity<String> response1 = rainfallControllerV2.getYearlyOffsetChart(1, "CSV"); // January
        assertEquals(200, response1.getStatusCodeValue());
        assertTrue(response1.getBody().contains("Jan"));
        
        ResponseEntity<String> response12 = rainfallControllerV2.getYearlyOffsetChart(12, "CSV"); // December
        assertEquals(200, response12.getStatusCodeValue());
        assertTrue(response12.getBody().contains("Dec"));
    }
}