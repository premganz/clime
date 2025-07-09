package com.example.clime.module.utility.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/utility")
@CrossOrigin(origins = "*")
public class BranchLocatorController {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private JsonNode branchData;

    public BranchLocatorController() {
        loadBranchData();
    }

    private void loadBranchData() {
        try {
            ClassPathResource resource = new ClassPathResource("branches.json");
            branchData = objectMapper.readTree(resource.getInputStream());
        } catch (IOException e) {
            System.err.println("Error loading branch data: " + e.getMessage());
            branchData = objectMapper.createObjectNode();
        }
    }

    @GetMapping("/branches")
    public JsonNode getAllBranches() {
        return branchData;
    }

    @GetMapping("/branches/search")
    public Map<String, Object> searchBranches(
            @RequestParam(required = false) String postcode,
            @RequestParam(required = false) String town,
            @RequestParam(required = false) String latitude,
            @RequestParam(required = false) String longitude,
            @RequestParam(defaultValue = "50") int radius) {
        
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> filteredData = new ArrayList<>();
        
        try {
            JsonNode dataArray = branchData.get("data");
            if (dataArray != null && dataArray.isArray()) {
                for (JsonNode dataItem : dataArray) {
                    JsonNode brands = dataItem.get("Brand");
                    if (brands != null && brands.isArray()) {
                        for (JsonNode brand : brands) {
                            JsonNode branches = brand.get("Branch");
                            if (branches != null && branches.isArray()) {
                                List<JsonNode> matchingBranches = new ArrayList<>();
                                
                                for (JsonNode branch : branches) {
                                    if (matchesCriteria(branch, postcode, town, latitude, longitude, radius)) {
                                        matchingBranches.add(branch);
                                    }
                                }
                                
                                if (!matchingBranches.isEmpty()) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> brandData = objectMapper.convertValue(brand, Map.class);
                                    brandData.put("Branch", matchingBranches.stream()
                                        .map(b -> {
                                            @SuppressWarnings("unchecked")
                                            Map<String, Object> branchMap = objectMapper.convertValue(b, Map.class);
                                            return branchMap;
                                        })
                                        .collect(Collectors.toList()));
                                    
                                    Map<String, Object> brandWrapper = new HashMap<>();
                                    brandWrapper.put("Brand", Arrays.asList(brandData));
                                    filteredData.add(brandWrapper);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error filtering branches: " + e.getMessage());
        }
        
        response.put("data", filteredData);
        
        // Add search metadata
        Map<String, Object> metadata = new HashMap<>();
        Map<String, Object> searchParams = new HashMap<>();
        searchParams.put("postcode", postcode != null ? postcode : "");
        searchParams.put("town", town != null ? town : "");
        searchParams.put("latitude", latitude != null ? latitude : "");
        searchParams.put("longitude", longitude != null ? longitude : "");
        searchParams.put("radius", radius);
        metadata.put("searchParams", searchParams);
        metadata.put("totalResults", filteredData.size());
        response.put("meta", metadata);
        
        return response;
    }

    private boolean matchesCriteria(JsonNode branch, String postcode, String town, String latitude, String longitude, int radius) {
        JsonNode postalAddress = branch.get("PostalAddress");
        if (postalAddress == null) return false;
        
        // If no search criteria provided, return all branches
        if ((postcode == null || postcode.trim().isEmpty()) &&
            (town == null || town.trim().isEmpty()) &&
            (latitude == null || latitude.trim().isEmpty()) &&
            (longitude == null || longitude.trim().isEmpty())) {
            return true;
        }
        
        // Check postcode match
        if (postcode != null && !postcode.trim().isEmpty()) {
            JsonNode postcodeNode = postalAddress.get("PostCode");
            if (postcodeNode != null) {
                String branchPostcode = postcodeNode.asText().toLowerCase();
                if (branchPostcode.contains(postcode.toLowerCase()) || 
                    postcode.toLowerCase().contains(branchPostcode)) {
                    return true;
                }
            }
        }
        
        // Check town match
        if (town != null && !town.trim().isEmpty()) {
            JsonNode townNode = postalAddress.get("TownName");
            if (townNode != null) {
                String branchTown = townNode.asText().toLowerCase();
                if (branchTown.contains(town.toLowerCase()) || 
                    town.toLowerCase().contains(branchTown)) {
                    return true;
                }
            }
        }
        
        // Check geographic proximity (simplified - in real implementation would use proper distance calculation)
        if (latitude != null && !latitude.trim().isEmpty() && 
            longitude != null && !longitude.trim().isEmpty()) {
            try {
                double searchLat = Double.parseDouble(latitude);
                double searchLon = Double.parseDouble(longitude);
                
                JsonNode geoLocation = postalAddress.get("GeoLocation");
                if (geoLocation != null) {
                    JsonNode coordinates = geoLocation.get("GeographicCoordinates");
                    if (coordinates != null) {
                        JsonNode latNode = coordinates.get("Latitude");
                        JsonNode lonNode = coordinates.get("Longitude");
                        if (latNode != null && lonNode != null) {
                            double branchLat = Double.parseDouble(latNode.asText());
                            double branchLon = Double.parseDouble(lonNode.asText());
                            
                            // Simple distance check (not accurate, but sufficient for demo)
                            double latDiff = Math.abs(searchLat - branchLat);
                            double lonDiff = Math.abs(searchLon - branchLon);
                            double approximateDistance = Math.sqrt(latDiff * latDiff + lonDiff * lonDiff) * 111; // rough km conversion
                            
                            if (approximateDistance <= radius) {
                                return true;
                            }
                        }
                    }
                }
            } catch (NumberFormatException e) {
                // Invalid coordinates, ignore this criteria
            }
        }
        
        return false;
    }

    @GetMapping("/branches/hello")
    public Map<String, String> hello() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Hello from Utility Module - Branch Locator!");
        response.put("service", "Open Banking UK Branch Locator API");
        response.put("specification", "https://openbankinguk.github.io/opendata-api-docs-pub/v2.4.0/branchlocator/branch-locator.html");
        return response;
    }
}
