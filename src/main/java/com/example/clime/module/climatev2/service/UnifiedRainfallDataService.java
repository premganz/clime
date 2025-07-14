package com.example.clime.module.climatev2.service;

import com.example.clime.module.climatev2.model.DataSource;
import com.example.clime.module.climatev2.model.RainfallRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service("unifiedRainfallDataService")
public class UnifiedRainfallDataService {
    
    @Autowired
    @Qualifier("rainfallDataServiceV2")
    private RainfallDataService csvDataService;
    
    @Autowired
    @Qualifier("kwsRainfallDataService")
    private KwsRainfallDataService kwsDataService;
    
    private DataSource currentDataSource = DataSource.CSV; // Default to CSV
    
    public void setDataSource(DataSource dataSource) {
        this.currentDataSource = dataSource;
    }
    
    public DataSource getCurrentDataSource() {
        return currentDataSource;
    }
    
    public List<RainfallRecord> getAllData() throws Exception {
        if (currentDataSource == DataSource.KWS) {
            return kwsDataService.getAllData();
        } else {
            return csvDataService.getAllData();
        }
    }
    
    public List<RainfallRecord> getDataByYear(int year) throws Exception {
        if (currentDataSource == DataSource.KWS) {
            return kwsDataService.getDataByYear(year);
        } else {
            return csvDataService.getDataByYear(year);
        }
    }
    
    public List<RainfallRecord> getDataByYearRange(int startYear, int endYear) throws Exception {
        if (currentDataSource == DataSource.KWS) {
            return kwsDataService.getDataByYearRange(startYear, endYear);
        } else {
            return csvDataService.getDataByYearRange(startYear, endYear);
        }
    }
    
    public String generateRainfallTableHtml(List<RainfallRecord> records) {
        if (currentDataSource == DataSource.KWS) {
            return kwsDataService.generateRainfallTableHtml(records);
        } else {
            return csvDataService.generateRainfallTableHtml(records);
        }
    }
    
    public Map<String, Object> getBasicStatistics() {
        if (currentDataSource == DataSource.CSV) {
            return csvDataService.getBasicStatistics();
        } else {
            // For KWS data, we'll need to calculate basic statistics
            // For now, return an empty map or delegate to CSV service
            return csvDataService.getBasicStatistics();
        }
    }
    
    public double getAverageRainfallForMonth(int month) {
        if (currentDataSource == DataSource.CSV) {
            return csvDataService.getAverageRainfallForMonth(month);
        } else {
            // For KWS data, calculate the average from the available data
            try {
                List<RainfallRecord> allData = kwsDataService.getAllData();
                return allData.stream()
                    .mapToDouble(record -> getMonthValue(record, month))
                    .average()
                    .orElse(0.0);
            } catch (Exception e) {
                System.err.println("Error calculating average rainfall for month " + month + ": " + e.getMessage());
                return 0.0;
            }
        }
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
    
    public String getDataSourceInfo() {
        StringBuilder info = new StringBuilder();
        info.append("<div class='alert alert-info'>");
        info.append("<strong>Current Data Source:</strong> ").append(currentDataSource.getDisplayName());
        info.append("<br><strong>Description:</strong> ").append(currentDataSource.getDescription());
        
        if (currentDataSource == DataSource.KWS) {
            String lastError = kwsDataService.getLastError();
            if (lastError != null) {
                info.append("<br><strong>Note:</strong> ").append(lastError);
                info.append(" Using mock data for demonstration.");
            }
        }
        
        info.append("</div>");
        return info.toString();
    }
}