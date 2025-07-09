package com.example.clime.module.utility3.service;

import com.example.clime.module.utility3.model.SpotQuoteRequest;
import com.example.clime.module.utility3.model.SpotQuoteResponse;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service("utility3SpotQuoteService")
public class SpotQuoteService {
    
    private static final Map<String, Double> BASE_RATES = new HashMap<>();
    private static final Map<String, Integer> DELIVERY_DAYS = new HashMap<>();
    private final Random random = new Random();
    
    static {
        // Base rates per kg for different transport modes (in USD)
        BASE_RATES.put("AIR", 8.5);
        BASE_RATES.put("SEA", 2.1);
        BASE_RATES.put("ROAD", 3.2);
        BASE_RATES.put("COURIER", 12.0);
        BASE_RATES.put("RAIL", 2.8);
        
        // Estimated delivery days
        DELIVERY_DAYS.put("AIR", 3);
        DELIVERY_DAYS.put("SEA", 21);
        DELIVERY_DAYS.put("ROAD", 7);
        DELIVERY_DAYS.put("COURIER", 2);
        DELIVERY_DAYS.put("RAIL", 10);
    }
    
    public SpotQuoteResponse calculateQuote(SpotQuoteRequest request) {
        String quoteId = generateQuoteId();
        String transportMode = request.getTransportMode().toUpperCase();
        
        // Calculate estimated cost
        Double baseRate = BASE_RATES.getOrDefault(transportMode, 5.0);
        Double weight = request.getWeightKg() != null ? request.getWeightKg() : 1.0;
        Integer packages = request.getNumberOfPackages() != null ? request.getNumberOfPackages() : 1;
        
        // Base calculation: (weight * base_rate) + (packages * handling_fee)
        Double handlingFee = getHandlingFee(transportMode);
        Double distanceFactor = getDistanceFactor(request.getDepartureAddress(), request.getDeliveryAddress());
        Double estimatedCost = (weight * baseRate * distanceFactor) + (packages * handlingFee);
        
        // Add random variation (±10%)
        Double variation = 0.9 + (random.nextDouble() * 0.2);
        estimatedCost = estimatedCost * variation;
        
        // Round to 2 decimal places
        estimatedCost = Math.round(estimatedCost * 100.0) / 100.0;
        
        // Get estimated delivery days
        Integer baseDays = DELIVERY_DAYS.getOrDefault(transportMode, 5);
        Integer estimatedDays = baseDays + random.nextInt(3); // Add 0-2 days variation
        
        String route = buildRoute(request.getDepartureAddress(), request.getDeliveryAddress(), transportMode);
        String notes = buildNotes(request);
        
        return new SpotQuoteResponse(
            quoteId,
            "ESTIMATED",
            estimatedCost,
            "USD",
            estimatedDays,
            transportMode,
            route,
            notes
        );
    }
    
    private String generateQuoteId() {
        return "SQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    private Double getHandlingFee(String transportMode) {
        switch (transportMode) {
            case "AIR": return 25.0;
            case "SEA": return 15.0;
            case "ROAD": return 20.0;
            case "COURIER": return 10.0;
            case "RAIL": return 18.0;
            default: return 20.0;
        }
    }
    
    private Double getDistanceFactor(String departure, String delivery) {
        // Simplified distance calculation based on string similarity
        if (departure == null || delivery == null) return 1.0;
        
        // If same city/region, apply local factor
        if (departure.toLowerCase().contains(delivery.toLowerCase()) || 
            delivery.toLowerCase().contains(departure.toLowerCase())) {
            return 0.5; // Local delivery
        }
        
        // Check for international shipping indicators
        if (isInternational(departure, delivery)) {
            return 2.5; // International factor
        }
        
        return 1.0; // Domestic factor
    }
    
    private boolean isInternational(String departure, String delivery) {
        String[] internationalKeywords = {"international", "usa", "uk", "europe", "asia", "africa", "australia"};
        String combined = (departure + " " + delivery).toLowerCase();
        
        for (String keyword : internationalKeywords) {
            if (combined.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    private String buildRoute(String departure, String delivery, String transportMode) {
        if (departure == null) departure = "Origin";
        if (delivery == null) delivery = "Destination";
        
        String via = getViaPoint(transportMode);
        return departure + " → " + via + " → " + delivery;
    }
    
    private String getViaPoint(String transportMode) {
        switch (transportMode) {
            case "AIR": return "Air Hub";
            case "SEA": return "Port";
            case "ROAD": return "Road Network";
            case "COURIER": return "Service Center";
            case "RAIL": return "Rail Terminal";
            default: return "Transit Hub";
        }
    }
    
    private String buildNotes(SpotQuoteRequest request) {
        StringBuilder notes = new StringBuilder();
        
        if (request.getDangerousGoodsInfo() != null && !request.getDangerousGoodsInfo().trim().isEmpty()) {
            notes.append("Special handling required for dangerous goods. ");
        }
        
        if (request.getWeightKg() != null && request.getWeightKg() > 1000) {
            notes.append("Heavy freight surcharge may apply. ");
        }
        
        if (request.getTypeOfGoods() != null && request.getTypeOfGoods().toLowerCase().contains("fragile")) {
            notes.append("Fragile goods handling recommended. ");
        }
        
        notes.append("Quote is estimated and subject to final verification. ");
        notes.append("Contact our freight experts for detailed pricing.");
        
        return notes.toString();
    }
}
