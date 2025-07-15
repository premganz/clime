package com.example.clime.controller;

import com.example.clime.module.climatev2.controller.RainfallControllerV2;
import com.example.clime.module.climatev2.controller.StatisticsControllerV2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class YearFilteringTest {

    @Autowired
    private RainfallControllerV2 rainfallControllerV2;

    @Autowired
    private StatisticsControllerV2 statisticsControllerV2;

    @Test
    public void testAnnualChartWithExcludedYears() {
        // Test annual chart with excluded years
        ResponseEntity<String> response = rainfallControllerV2.getAnnualChart("CSV", "1940,1965,2015");
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        // Should contain information about excluded years
        assertTrue(response.getBody().contains("Excluded Years") || response.getBody().contains("filters"), 
                   "Should indicate that year filtering is applied");
    }

    @Test
    public void testMonthlyChartWithExcludedYears() {
        // Test monthly chart with excluded years
        ResponseEntity<String> response = rainfallControllerV2.getMonthlyChart("CSV", "1940,1965");
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        // Should not contain error messages
        assertFalse(response.getBody().contains("alert-danger"), "Should not contain error messages");
    }

    @Test
    public void testBundledChartWithExcludedYears() {
        // Test bundled chart with excluded years
        ResponseEntity<String> response = rainfallControllerV2.getDecadeOffsetChart(0, 10, "CSV", "1940,1965,2015");
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        // Should contain chart content without errors
        assertFalse(response.getBody().contains("alert-danger"), "Should not contain error messages");
        assertTrue(response.getBody().contains("Bundle") || response.getBody().contains("chart"), 
                   "Should contain chart content");
    }

    @Test
    public void testYearWiseChartWithExcludedYears() {
        // Test year-wise chart with excluded years
        ResponseEntity<String> response = rainfallControllerV2.getYearWiseChart("CSV", "1940,1965");
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        // Should not contain error messages
        assertFalse(response.getBody().contains("alert-danger"), "Should not contain error messages");
    }

    @Test
    public void testInvalidExcludedYearsFormat() {
        // Test with invalid excluded years format
        ResponseEntity<String> response = rainfallControllerV2.getAnnualChart("CSV", "abc,xyz,123.5");
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        // Should handle invalid format gracefully without errors
        assertFalse(response.getBody().contains("Exception"), "Should not contain exception messages");
    }

    @Test
    public void testEmptyExcludedYears() {
        // Test with empty excluded years
        ResponseEntity<String> response = rainfallControllerV2.getAnnualChart("CSV", "");
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        // Should work normally without filtering
        assertFalse(response.getBody().contains("alert-danger"), "Should not contain error messages");
    }

    @Test
    public void testNullExcludedYears() {
        // Test with null excluded years
        ResponseEntity<String> response = rainfallControllerV2.getAnnualChart("CSV", null);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        // Should work normally without filtering
        assertFalse(response.getBody().contains("alert-danger"), "Should not contain error messages");
    }
}