package com.example.clime.module.climatev2.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RainfallAnalyticsServiceTest {

    @Autowired
    @Qualifier("rainfallAnalyticsServiceV2")
    private RainfallAnalyticsService rainfallAnalyticsService;

    @Test
    void testGenerateDecadeComparisonChartHtmlWithDifferentBundleSizes() {
        // Test with default 10-year bundle
        String html10 = rainfallAnalyticsService.generateDecadeComparisonChartHtml(10);
        assertNotNull(html10);
        assertTrue(html10.contains("Decade-wise Rainfall Comparison"));
        assertTrue(html10.contains("1900s"));
        assertTrue(html10.contains("Decade"));

        // Test with 5-year bundle
        String html5 = rainfallAnalyticsService.generateDecadeComparisonChartHtml(5);
        assertNotNull(html5);
        assertTrue(html5.contains("5-Year Bundled Rainfall Comparison"));
        assertTrue(html5.contains("1900-1904"));
        assertTrue(html5.contains("Period"));

        // Test with 20-year bundle
        String html20 = rainfallAnalyticsService.generateDecadeComparisonChartHtml(20);
        assertNotNull(html20);
        assertTrue(html20.contains("20-Year Bundled Rainfall Comparison"));
        assertTrue(html20.contains("1900-1919"));
        assertTrue(html20.contains("Period"));
    }

    @Test
    void testGenerateDecadeComparisonChartHtmlWithOffsetAndBundleSize() {
        // Test with 5-year bundle and offset 3
        String html = rainfallAnalyticsService.generateDecadeComparisonChartHtmlWithOffset(3, 5);
        assertNotNull(html);
        assertTrue(html.contains("5-Year Bundled Rainfall Comparison (Offset: 3)"));
        assertTrue(html.contains("1903-1907"));
        assertTrue(html.contains("Interval"));
        
        // Test with default 10-year bundle and offset 5
        String html10 = rainfallAnalyticsService.generateDecadeComparisonChartHtmlWithOffset(5, 10);
        assertNotNull(html10);
        assertTrue(html10.contains("Decade-wise Rainfall Comparison (Offset: 5)"));
        assertTrue(html10.contains("1905-1914"));
    }

    @Test
    void testBundleSizeValidation() {
        // Test with invalid bundle size (too small)
        String htmlSmall = rainfallAnalyticsService.generateDecadeComparisonChartHtml(0);
        assertNotNull(htmlSmall);
        assertTrue(htmlSmall.contains("Decade-wise Rainfall Comparison")); // Should default to 10

        // Test with invalid bundle size (too large)
        String htmlLarge = rainfallAnalyticsService.generateDecadeComparisonChartHtml(100);
        assertNotNull(htmlLarge);
        assertTrue(htmlLarge.contains("50-Year Bundled Rainfall Comparison")); // Should cap at 50
    }
}