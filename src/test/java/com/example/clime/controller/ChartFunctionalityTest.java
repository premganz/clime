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
        ResponseEntity<String> response = rainfallControllerV2.getAnnualChart("CSV", null, null, null, null);
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
        ResponseEntity<String> response = rainfallControllerV2.getAnnualChart("KWS", null, null, null, null);
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
    public void testBundledChartWithZeroOffset() {
        // Test bundled chart with zero offset - this replaces the removed decade chart functionality
        ResponseEntity<String> response = rainfallControllerV2.getDecadeOffsetChart(0, 10, "KWS", null, null, null, null);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        // Should not contain the restriction message
        assertFalse(response.getBody().contains("Chart not available for KWS data"));
        
        // Should contain chart content with offset 0 (equivalent to old bundled chart)
        assertTrue(response.getBody().contains("(Offset: 0)"), "Should contain offset in title");
        assertTrue(response.getBody().contains("Bundle Size:</strong> 10 years"), "Should contain bundle size in summary");
    }

    @Test
    public void testBundledChartFunctionality() {
        // Test bundled chart with different bundle sizes using offset 0 (replaces old bundled chart)
        ResponseEntity<String> response5Years = rainfallControllerV2.getDecadeOffsetChart(0, 5, "CSV", null, null, null, null);
        assertEquals(200, response5Years.getStatusCodeValue());
        assertNotNull(response5Years.getBody());
        assertTrue(response5Years.getBody().contains("5-Year Bundle"), "Should contain bundle size in title");
        assertTrue(response5Years.getBody().contains("Bundle Size:</strong> 5 years"), "Should contain bundle size in summary");
        
        ResponseEntity<String> response10Years = rainfallControllerV2.getDecadeOffsetChart(0, 10, "CSV", null, null, null, null);
        assertEquals(200, response10Years.getStatusCodeValue());
        assertNotNull(response10Years.getBody());
        assertTrue(response10Years.getBody().contains("10-Year Bundle"), "Should contain bundle size in title");
        assertTrue(response10Years.getBody().contains("Bundle Size:</strong> 10 years"), "Should contain bundle size in summary");
    }

    @Test
    public void testBundledChartWithOffset() {
        // Test bundled chart with offset functionality
        ResponseEntity<String> response = rainfallControllerV2.getDecadeOffsetChart(2, 5, "CSV", null, null, null, null);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("(Offset: 2)"), "Should contain offset in title");
        assertTrue(response.getBody().contains("Bundle Size:</strong> 5 years"), "Should contain bundle size in summary");
        assertTrue(response.getBody().contains("Offset:</strong> 2 years"), "Should contain offset in summary");
    }

    @Test
    public void testMonthlyChart() {
        // Test monthly chart with KWS data
        ResponseEntity<String> response = rainfallControllerV2.getMonthlyChart("KWS", null, null, null, null);
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
    public void testBundledChartWithInvalidOffset() {
        // Test bundled chart with invalid offset - should return error message
        ResponseEntity<String> response = rainfallControllerV2.getDecadeOffsetChart(10, 5, "CSV", null, null, null, null);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        // Should contain error message for invalid offset (offset >= bundleSize)
        assertTrue(response.getBody().contains("Invalid Offset"), "Should contain invalid offset error message");
        assertTrue(response.getBody().contains("alert-danger"), "Should contain error styling");
        assertTrue(response.getBody().contains("must be strictly less than bundle size"), "Should contain specific error message");
    }

    @Test
    public void testBundledChartWithNegativeOffset() {
        // Test bundled chart with negative offset - should return error message
        ResponseEntity<String> response = rainfallControllerV2.getDecadeOffsetChart(-1, 5, "CSV", null, null, null, null);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        // Should contain error message for negative offset
        assertTrue(response.getBody().contains("Invalid Offset"), "Should contain invalid offset error message");
        assertTrue(response.getBody().contains("alert-danger"), "Should contain error styling");
        assertTrue(response.getBody().contains("cannot be negative"), "Should contain specific error message for negative offset");
    }

    @Test
    public void testBundledChartWithValidOffset() {
        // Test bundled chart with valid offset - should work normally
        ResponseEntity<String> response = rainfallControllerV2.getDecadeOffsetChart(4, 5, "CSV", null, null, null, null);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        // Should NOT contain error message
        assertFalse(response.getBody().contains("Invalid Offset"), "Should not contain invalid offset error message");
        assertFalse(response.getBody().contains("alert-danger"), "Should not contain error styling");
        
        // Should contain chart content
        assertTrue(response.getBody().contains("(Offset: 4)"), "Should contain offset in title");
        assertTrue(response.getBody().contains("Bundle Size:</strong> 5 years"), "Should contain bundle size in summary");
        assertTrue(response.getBody().contains("Offset:</strong> 4 years"), "Should contain offset in summary");
    }
}