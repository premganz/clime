package com.example.clime.module.climatev2.service;

import com.example.clime.module.climatev2.model.DataSource;
import com.example.clime.module.climatev2.model.RainfallRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ExcludedYearsTest {

    @Autowired
    private UnifiedRainfallDataService unifiedRainfallDataService;

    @Test
    public void testExcludedYearsFiltering() throws Exception {
        // Set to CSV data source
        unifiedRainfallDataService.setDataSource(DataSource.CSV);
        
        // Get all data without exclusions
        List<RainfallRecord> allData = unifiedRainfallDataService.getAllData();
        int originalCount = allData.size();
        
        // Test with excluded years
        String excludedYears = "1940,1965,2015";
        List<RainfallRecord> filteredData = unifiedRainfallDataService.getAllData(excludedYears);
        
        // Should have 3 fewer records
        assertEquals(originalCount - 3, filteredData.size());
        
        // Verify that excluded years are not in the filtered data
        boolean hasExcludedYear = filteredData.stream()
            .anyMatch(record -> record.getYear() == 1940 || record.getYear() == 1965 || record.getYear() == 2015);
        assertFalse(hasExcludedYear, "Filtered data should not contain excluded years");
    }

    @Test
    public void testExcludedYearsInfo() {
        String excludedYears = "1940,1965,2015";
        String info = unifiedRainfallDataService.getExcludedYearsInfo(excludedYears);
        
        assertNotNull(info);
        assertTrue(info.contains("1940"));
        assertTrue(info.contains("1965"));
        assertTrue(info.contains("2015"));
        assertTrue(info.contains("3 years excluded"));
    }

    @Test
    public void testEmptyExcludedYears() throws Exception {
        // Set to CSV data source
        unifiedRainfallDataService.setDataSource(DataSource.CSV);
        
        List<RainfallRecord> allData = unifiedRainfallDataService.getAllData();
        List<RainfallRecord> filteredData = unifiedRainfallDataService.getAllData("");
        
        // Should be the same size
        assertEquals(allData.size(), filteredData.size());
        
        // Test info with empty string
        String info = unifiedRainfallDataService.getExcludedYearsInfo("");
        assertEquals("", info);
    }

    @Test
    public void testInvalidExcludedYearsFormat() {
        String invalidYears = "abc,xyz,123.5";
        String info = unifiedRainfallDataService.getExcludedYearsInfo(invalidYears);
        
        assertNotNull(info);
        assertTrue(info.contains("Invalid Excluded Years Format"));
    }
}