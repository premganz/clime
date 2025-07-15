package com.example.clime.controller;

import com.example.clime.module.climatev2.controller.RainfallControllerV2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class DateRangeFilteringTest {

    @Autowired
    private RainfallControllerV2 rainfallControllerV2;

    @Test
    public void testBundledChartWithYearFilter() {
        // Test bundled chart with single year filter
        ResponseEntity<String> response = rainfallControllerV2.getDecadeOffsetChart(0, 10, "CSV", null, 2010, null, null);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        // Should contain year filter information
        assertTrue(response.getBody().contains("Year Filter") || response.getBody().contains("2010"), 
                   "Should indicate year filtering is applied");
        
        // Should not contain error messages
        assertFalse(response.getBody().contains("alert-danger"), "Should not contain error messages");
    }

    @Test
    public void testBundledChartWithYearRangeFilter() {
        // Test bundled chart with year range filter
        ResponseEntity<String> response = rainfallControllerV2.getDecadeOffsetChart(0, 10, "CSV", null, null, 2000, 2020);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        // Should contain date range filter information
        assertTrue(response.getBody().contains("Date Range Filter") || response.getBody().contains("2000") && response.getBody().contains("2020"), 
                   "Should indicate date range filtering is applied");
        
        // Should not contain error messages
        assertFalse(response.getBody().contains("alert-danger"), "Should not contain error messages");
    }

    @Test
    public void testBundledChartWithCombinedFilters() {
        // Test bundled chart with both excluded years and year range
        ResponseEntity<String> response = rainfallControllerV2.getDecadeOffsetChart(0, 10, "CSV", "1940,1965", null, 2000, 2020);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        // Should contain information about both filters
        assertTrue(response.getBody().contains("Excluded Years") || response.getBody().contains("Date Range Filter"), 
                   "Should indicate filtering is applied");
        
        // Should not contain error messages
        assertFalse(response.getBody().contains("alert-danger"), "Should not contain error messages");
    }

    @Test
    public void testAnnualChartWithYearFilter() {
        // Test annual chart with single year filter
        ResponseEntity<String> response = rainfallControllerV2.getAnnualChart("CSV", null, 2015, null, null);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        // Should contain year filter information
        assertTrue(response.getBody().contains("Year Filter") || response.getBody().contains("2015"), 
                   "Should indicate year filtering is applied");
        
        // Should not contain error messages
        assertFalse(response.getBody().contains("alert-danger"), "Should not contain error messages");
    }

    @Test
    public void testAnnualChartWithYearRangeFilter() {
        // Test annual chart with year range filter
        ResponseEntity<String> response = rainfallControllerV2.getAnnualChart("CSV", null, null, 1990, 2000);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        // Should contain date range filter information
        assertTrue(response.getBody().contains("Date Range Filter") || response.getBody().contains("1990") && response.getBody().contains("2000"), 
                   "Should indicate date range filtering is applied");
        
        // Should not contain error messages
        assertFalse(response.getBody().contains("alert-danger"), "Should not contain error messages");
    }

    @Test
    public void testMonthlyChartWithFilters() {
        // Test monthly chart with year range and excluded years
        ResponseEntity<String> response = rainfallControllerV2.getMonthlyChart("CSV", "1940,1965", null, 1950, 2000);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        // Should not contain error messages
        assertFalse(response.getBody().contains("alert-danger"), "Should not contain error messages");
    }

    @Test 
    public void testYearWiseChartWithYearRangeFilter() {
        // Test year-wise chart with year range filter
        ResponseEntity<String> response = rainfallControllerV2.getYearWiseChart("CSV", null, null, 2010, 2020);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        // Should not contain error messages
        assertFalse(response.getBody().contains("alert-danger"), "Should not contain error messages");
    }
}