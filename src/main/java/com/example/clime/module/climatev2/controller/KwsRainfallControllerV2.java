package com.example.clime.module.climatev2.controller;

import com.example.clime.module.climatev2.model.RainfallRecord;
import com.example.clime.module.climatev2.service.KwsRainfallDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kws-rainfall")
public class KwsRainfallControllerV2 {

    @Autowired
    private KwsRainfallDataService kwsRainfallDataService;

    @GetMapping("/data")
    public ResponseEntity<String> getKwsRainfallData(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer startYear,
            @RequestParam(required = false) Integer endYear) {
        try {
            List<RainfallRecord> records;
            
            if (year != null) {
                records = kwsRainfallDataService.getDataByYear(year);
            } else if (startYear != null && endYear != null) {
                records = kwsRainfallDataService.getDataByYearRange(startYear, endYear);
            } else {
                records = kwsRainfallDataService.getAllData();
            }
            
            if (records.isEmpty()) {
                return ResponseEntity.ok("<div class='alert alert-warning'>No KWS rainfall data found for the specified criteria.</div>");
            }
            
            String html = kwsRainfallDataService.generateRainfallTableHtml(records);
            return ResponseEntity.ok(html);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok("<div class='alert alert-danger'>Error retrieving KWS rainfall data: " + e.getMessage() + "</div>");
        }
    }

    @GetMapping("/status")
    public ResponseEntity<String> getKwsDataStatus() {
        try {
            if (kwsRainfallDataService.isDataLoaded()) {
                List<RainfallRecord> allData = kwsRainfallDataService.getAllData();
                int dataSize = allData.size();
                int minYear = allData.stream().mapToInt(RainfallRecord::getYear).min().orElse(0);
                int maxYear = allData.stream().mapToInt(RainfallRecord::getYear).max().orElse(0);
                
                return ResponseEntity.ok(String.format(
                    "<div class='alert alert-success'>" +
                    "<strong>✅ KWS rainfall analytics loaded successfully!</strong><br>" +
                    "Data range: %d-%d (%d years)<br>" +
                    "Data source: KWS Chennai Local CSV Data<br>" +
                    "<small>Note: Data extracted from KWS Chennai website and stored locally for reliable access</small>" +
                    "</div>", 
                    minYear, maxYear, dataSize
                ));
            } else {
                String error = kwsRainfallDataService.getLastError();
                return ResponseEntity.ok(
                    "<div class='alert alert-danger'>" +
                    "<strong>❌ KWS rainfall data not loaded</strong><br>" +
                    "Error: " + (error != null ? error : "Unknown error") +
                    "</div>"
                );
            }
        } catch (Exception e) {
            return ResponseEntity.ok(
                "<div class='alert alert-danger'>" +
                "<strong>❌ Error checking KWS data status</strong><br>" +
                "Error: " + e.getMessage() +
                "</div>"
            );
        }
    }
}