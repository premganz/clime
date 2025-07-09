package com.example.clime.module.utility2.model;

import java.time.LocalDateTime;
import java.util.List;

public class AirFreightQuoteResponse {
    private String quoteId;
    private String status;
    private Double estimatedCost;
    private String currency;
    private Integer estimatedDays;
    private String transportMode;
    private String route;
    private LocalDateTime quoteDateTime;
    private LocalDateTime validUntil;
    private String notes;
    
    // Air freight specific details
    private String airlineCarrier;
    private String flightRoute;
    private Double chargeableWeight; // kg (higher of actual weight or volumetric weight)
    private Double volumetricWeight; // kg
    private Double volumetricWeightFactor; // typically 167 for air freight
    private String serviceType; // Express, Standard, Economy
    private List<String> specialServices;
    private Double fuelSurcharge;
    private Double securityFee;
    private Double handlingFee;
    private Double totalCharges;
    
    // Pickup and delivery details
    private String pickupTimeWindow;
    private String deliveryTimeWindow;
    private String transitDetails;
    
    // Constructors
    public AirFreightQuoteResponse() {}
    
    public AirFreightQuoteResponse(String quoteId, String status, Double estimatedCost, 
                                 String currency, Integer estimatedDays, String transportMode, 
                                 String route, String notes) {
        this.quoteId = quoteId;
        this.status = status;
        this.estimatedCost = estimatedCost;
        this.currency = currency;
        this.estimatedDays = estimatedDays;
        this.transportMode = transportMode;
        this.route = route;
        this.notes = notes;
        this.quoteDateTime = LocalDateTime.now();
        this.validUntil = LocalDateTime.now().plusDays(7);
    }
    
    // Getters and Setters
    public String getQuoteId() { return quoteId; }
    public void setQuoteId(String quoteId) { this.quoteId = quoteId; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Double getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(Double estimatedCost) { this.estimatedCost = estimatedCost; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public Integer getEstimatedDays() { return estimatedDays; }
    public void setEstimatedDays(Integer estimatedDays) { this.estimatedDays = estimatedDays; }
    
    public String getTransportMode() { return transportMode; }
    public void setTransportMode(String transportMode) { this.transportMode = transportMode; }
    
    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }
    
    public LocalDateTime getQuoteDateTime() { return quoteDateTime; }
    public void setQuoteDateTime(LocalDateTime quoteDateTime) { this.quoteDateTime = quoteDateTime; }
    
    public LocalDateTime getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDateTime validUntil) { this.validUntil = validUntil; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public String getAirlineCarrier() { return airlineCarrier; }
    public void setAirlineCarrier(String airlineCarrier) { this.airlineCarrier = airlineCarrier; }
    
    public String getFlightRoute() { return flightRoute; }
    public void setFlightRoute(String flightRoute) { this.flightRoute = flightRoute; }
    
    public Double getChargeableWeight() { return chargeableWeight; }
    public void setChargeableWeight(Double chargeableWeight) { this.chargeableWeight = chargeableWeight; }
    
    public Double getVolumetricWeight() { return volumetricWeight; }
    public void setVolumetricWeight(Double volumetricWeight) { this.volumetricWeight = volumetricWeight; }
    
    public Double getVolumetricWeightFactor() { return volumetricWeightFactor; }
    public void setVolumetricWeightFactor(Double volumetricWeightFactor) { this.volumetricWeightFactor = volumetricWeightFactor; }
    
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    
    public List<String> getSpecialServices() { return specialServices; }
    public void setSpecialServices(List<String> specialServices) { this.specialServices = specialServices; }
    
    public Double getFuelSurcharge() { return fuelSurcharge; }
    public void setFuelSurcharge(Double fuelSurcharge) { this.fuelSurcharge = fuelSurcharge; }
    
    public Double getSecurityFee() { return securityFee; }
    public void setSecurityFee(Double securityFee) { this.securityFee = securityFee; }
    
    public Double getHandlingFee() { return handlingFee; }
    public void setHandlingFee(Double handlingFee) { this.handlingFee = handlingFee; }
    
    public Double getTotalCharges() { return totalCharges; }
    public void setTotalCharges(Double totalCharges) { this.totalCharges = totalCharges; }
    
    public String getPickupTimeWindow() { return pickupTimeWindow; }
    public void setPickupTimeWindow(String pickupTimeWindow) { this.pickupTimeWindow = pickupTimeWindow; }
    
    public String getDeliveryTimeWindow() { return deliveryTimeWindow; }
    public void setDeliveryTimeWindow(String deliveryTimeWindow) { this.deliveryTimeWindow = deliveryTimeWindow; }
    
    public String getTransitDetails() { return transitDetails; }
    public void setTransitDetails(String transitDetails) { this.transitDetails = transitDetails; }
}
