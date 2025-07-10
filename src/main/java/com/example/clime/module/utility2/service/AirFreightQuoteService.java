package com.example.clime.module.utility2.service;

import com.example.clime.module.utility2.model.AirFreightQuoteRequest;
import com.example.clime.module.utility2.model.AirFreightQuoteResponse;
import org.springframework.stereotype.Service;

import java.util.*;

@Service("airFreightQuoteService")
public class AirFreightQuoteService {
    
    private static final Map<String, Double> AIR_FREIGHT_RATES = new HashMap<>();
    private static final Map<String, String[]> AIRPORT_CODES = new HashMap<>();
    private static final Map<String, String> AIRLINE_CARRIERS = new HashMap<>();
    private static final Double VOLUMETRIC_FACTOR = 167.0; // kg/m³ for air freight
    private final Random random = new Random();
    
    static {
        // Base rates per kg for different routes (in USD)
        AIR_FREIGHT_RATES.put("DOMESTIC", 4.5);
        AIR_FREIGHT_RATES.put("REGIONAL", 6.8);
        AIR_FREIGHT_RATES.put("INTERCONTINENTAL", 8.2);
        AIR_FREIGHT_RATES.put("EXPRESS", 12.5);
        
        // Airport codes for major cities
        AIRPORT_CODES.put("India", new String[]{"DEL", "BOM", "BLR", "MAA", "CCU", "HYD"});
        AIRPORT_CODES.put("United States", new String[]{"JFK", "LAX", "ORD", "ATL", "DFW", "DEN"});
        AIRPORT_CODES.put("United Kingdom", new String[]{"LHR", "LGW", "MAN", "EDI", "BHX", "STN"});
        AIRPORT_CODES.put("Germany", new String[]{"FRA", "MUC", "DUS", "TXL", "HAM", "STR"});
        AIRPORT_CODES.put("France", new String[]{"CDG", "ORY", "NCE", "LYS", "TLS", "BOD"});
        AIRPORT_CODES.put("China", new String[]{"PEK", "PVG", "CAN", "SZX", "CTU", "XIY"});
        AIRPORT_CODES.put("Japan", new String[]{"NRT", "HND", "KIX", "NGO", "FUK", "CTS"});
        AIRPORT_CODES.put("Australia", new String[]{"SYD", "MEL", "BNE", "PER", "ADL", "DRW"});
        
        // Airline carriers
        AIRLINE_CARRIERS.put("PREMIUM", "Lufthansa Cargo");
        AIRLINE_CARRIERS.put("STANDARD", "Emirates SkyCargo");
        AIRLINE_CARRIERS.put("ECONOMY", "Turkish Cargo");
        AIRLINE_CARRIERS.put("EXPRESS", "DHL Aviation");
    }
    
    public AirFreightQuoteResponse calculateAirFreightQuote(AirFreightQuoteRequest request) {
        String quoteId = generateQuoteId();
        
        // Calculate weights
        Double actualWeight = request.getTotalWeight() != null ? request.getTotalWeight() : 0.0;
        Double totalVolume = request.getTotalVolume() != null ? request.getTotalVolume() : 0.0;
        Double volumetricWeight = totalVolume * VOLUMETRIC_FACTOR;
        Double chargeableWeight = Math.max(actualWeight, volumetricWeight);
        
        // Determine route type and base rate
        String routeType = determineRouteType(request.getOriginCountry(), request.getDestinationCountry());
        Double baseRate = AIR_FREIGHT_RATES.get(routeType);
        
        // Calculate base freight cost
        Double baseCost = chargeableWeight * baseRate;
        
        // Add surcharges
        Double fuelSurcharge = baseCost * 0.15; // 15% fuel surcharge
        Double securityFee = Math.min(chargeableWeight * 0.35, 50.0); // Security fee with cap
        Double handlingFee = calculateHandlingFee(request);
        
        // Special services surcharges
        List<String> specialServices = new ArrayList<>();
        Double specialServicesCost = 0.0;
        
        if (Boolean.TRUE.equals(request.getDangerousGoods())) {
            specialServices.add("Dangerous Goods Handling");
            specialServicesCost += chargeableWeight * 2.5;
        }
        
        if (Boolean.TRUE.equals(request.getTemperatureControlled())) {
            specialServices.add("Temperature Controlled");
            specialServicesCost += chargeableWeight * 1.8;
        }
        
        if (Boolean.TRUE.equals(request.getHighValue())) {
            specialServices.add("High Value Cargo");
            specialServicesCost += baseCost * 0.05; // 5% of base cost
        }
        
        // Calculate total
        Double totalCost = baseCost + fuelSurcharge + securityFee + handlingFee + specialServicesCost;
        
        // Add random variation (±8%)
        Double variation = 0.92 + (random.nextDouble() * 0.16);
        totalCost = totalCost * variation;
        
        // Round to 2 decimal places
        totalCost = Math.round(totalCost * 100.0) / 100.0;
        
        // Determine transit time
        Integer transitDays = calculateTransitDays(routeType, request);
        
        // Create response
        AirFreightQuoteResponse response = new AirFreightQuoteResponse(
            quoteId, "ESTIMATED", totalCost, "USD", transitDays, "AIR",
            buildFlightRoute(request), buildNotes(request)
        );
        
        // Set air freight specific details
        response.setChargeableWeight(chargeableWeight);
        response.setVolumetricWeight(volumetricWeight);
        response.setVolumetricWeightFactor(VOLUMETRIC_FACTOR);
        response.setAirlineCarrier(selectCarrier(routeType));
        response.setFlightRoute(buildDetailedRoute(request));
        response.setServiceType(determineServiceType(request));
        response.setSpecialServices(specialServices);
        response.setFuelSurcharge(fuelSurcharge);
        response.setSecurityFee(securityFee);
        response.setHandlingFee(handlingFee);
        response.setTotalCharges(totalCost);
        response.setPickupTimeWindow("08:00 - 17:00");
        response.setDeliveryTimeWindow("09:00 - 18:00");
        response.setTransitDetails(buildTransitDetails(transitDays, routeType));
        
        return response;
    }
    
    private String generateQuoteId() {
        return "AF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    private String determineRouteType(String origin, String destination) {
        if (origin == null || destination == null) return "REGIONAL";
        
        if (origin.equals(destination)) {
            return "DOMESTIC";
        }
        
        // Define regional groupings
        Set<String> asia = new HashSet<>(Arrays.asList("India", "China", "Japan", "Thailand", "Singapore", "Malaysia"));
        Set<String> europe = new HashSet<>(Arrays.asList("United Kingdom", "Germany", "France", "Netherlands", "Italy", "Spain"));
        Set<String> americas = new HashSet<>(Arrays.asList("United States", "Canada", "Mexico", "Brazil", "Argentina"));
        Set<String> oceania = new HashSet<>(Arrays.asList("Australia", "New Zealand"));
        
        for (Set<String> region : Arrays.asList(asia, europe, americas, oceania)) {
            if (region.contains(origin) && region.contains(destination)) {
                return "REGIONAL";
            }
        }
        
        return "INTERCONTINENTAL";
    }
    
    private Double calculateHandlingFee(AirFreightQuoteRequest request) {
        Double baseHandling = 45.0; // Base handling fee
        
        if (request.getCargoItems() != null && request.getCargoItems().size() > 1) {
            baseHandling += (request.getCargoItems().size() - 1) * 15.0; // Additional pieces
        }
        
        return baseHandling;
    }
    
    private Integer calculateTransitDays(String routeType, AirFreightQuoteRequest request) {
        int baseDays;
        switch (routeType) {
            case "DOMESTIC": baseDays = 1; break;
            case "REGIONAL": baseDays = 2; break;
            case "INTERCONTINENTAL": baseDays = 3; break;
            case "EXPRESS": baseDays = 1; break;
            default: baseDays = 3;
        }
        
        // Add variation
        int variation = random.nextInt(2); // 0-1 days
        return baseDays + variation;
    }
    
    private String selectCarrier(String routeType) {
        if ("EXPRESS".equals(routeType)) {
            return AIRLINE_CARRIERS.get("EXPRESS");
        }
        
        String[] carrierTypes = {"PREMIUM", "STANDARD", "ECONOMY"};
        String selectedType = carrierTypes[random.nextInt(carrierTypes.length)];
        return AIRLINE_CARRIERS.get(selectedType);
    }
    
    private String buildFlightRoute(AirFreightQuoteRequest request) {
        String origin = request.getOriginCity() != null ? request.getOriginCity() : request.getOriginCountry();
        String destination = request.getDestinationCity() != null ? request.getDestinationCity() : request.getDestinationCountry();
        return origin + " → " + destination;
    }
    
    private String buildDetailedRoute(AirFreightQuoteRequest request) {
        String originCode = getRandomAirportCode(request.getOriginCountry());
        String destCode = getRandomAirportCode(request.getDestinationCountry());
        
        // Add potential hub for intercontinental routes
        String routeType = determineRouteType(request.getOriginCountry(), request.getDestinationCountry());
        if ("INTERCONTINENTAL".equals(routeType)) {
            String[] hubs = {"FRA", "DXB", "SIN", "ICN", "AMS"};
            String hub = hubs[random.nextInt(hubs.length)];
            return originCode + " → " + hub + " → " + destCode;
        }
        
        return originCode + " → " + destCode;
    }
    
    private String getRandomAirportCode(String country) {
        String[] codes = AIRPORT_CODES.get(country);
        if (codes != null && codes.length > 0) {
            return codes[random.nextInt(codes.length)];
        }
        return "XXX"; // Default code
    }
    
    private String determineServiceType(AirFreightQuoteRequest request) {
        if (Boolean.TRUE.equals(request.getDangerousGoods()) || 
            Boolean.TRUE.equals(request.getHighValue())) {
            return "Premium";
        }
        
        if (Boolean.TRUE.equals(request.getTemperatureControlled())) {
            return "Express";
        }
        
        return "Standard";
    }
    
    private String buildTransitDetails(Integer days, String routeType) {
        switch (routeType) {
            case "DOMESTIC":
                return "Next day delivery within domestic network";
            case "REGIONAL":
                return "Direct flight or single connection within region";
            case "INTERCONTINENTAL":
                return "Intercontinental service with potential hub connection";
            default:
                return "Standard air freight service with " + days + " days transit time";
        }
    }
    
    private String buildNotes(AirFreightQuoteRequest request) {
        StringBuilder notes = new StringBuilder();
        
        notes.append("Quote valid for 7 days. ");
        
        if (Boolean.TRUE.equals(request.getDangerousGoods())) {
            notes.append("DG documentation required. ");
        }
        
        if (Boolean.TRUE.equals(request.getTemperatureControlled())) {
            notes.append("Cold chain certification available. ");
        }
        
        if (request.getTotalWeight() != null && request.getTotalWeight() > 500) {
            notes.append("Heavy cargo handling included. ");
        }
        
        notes.append("Rates subject to fuel and security surcharges. ");
        notes.append("Door-to-door service available upon request.");
        
        return notes.toString();
    }
}
