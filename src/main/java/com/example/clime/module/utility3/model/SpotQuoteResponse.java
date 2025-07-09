package com.example.clime.module.utility3.model;

import java.time.LocalDateTime;

public class SpotQuoteResponse {
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
    
    // Constructors
    public SpotQuoteResponse() {}
    
    public SpotQuoteResponse(String quoteId, String status, Double estimatedCost, 
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
        this.validUntil = LocalDateTime.now().plusDays(7); // Valid for 7 days
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
}
