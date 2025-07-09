package com.example.clime.module.utility2.model;

import java.time.LocalDate;
import java.util.List;

public class AirFreightQuoteRequest {
    
    // Origin details
    private String originCountry;
    private String originPostal;
    private String originCity;
    
    // Destination details
    private String destinationCountry;
    private String destinationPostal;
    private String destinationCity;
    
    // Cargo details
    private List<CargoItem> cargoItems;
    private Double totalWeight;
    private Double totalVolume;
    
    // Terms and payment
    private String incoterm;
    private String paymentTerms; // prepaid, collect
    
    // Special requirements
    private Boolean dangerousGoods;
    private Boolean temperatureControlled;
    private Boolean highValue;
    private String specialInstructions;
    
    // Transport mode
    private String transportMode; // AIR
    
    // Additional details
    private LocalDate preferredPickupDate;
    private LocalDate requiredDeliveryDate;
    
    // Constructors
    public AirFreightQuoteRequest() {}
    
    // Getters and Setters
    public String getOriginCountry() { return originCountry; }
    public void setOriginCountry(String originCountry) { this.originCountry = originCountry; }
    
    public String getOriginPostal() { return originPostal; }
    public void setOriginPostal(String originPostal) { this.originPostal = originPostal; }
    
    public String getOriginCity() { return originCity; }
    public void setOriginCity(String originCity) { this.originCity = originCity; }
    
    public String getDestinationCountry() { return destinationCountry; }
    public void setDestinationCountry(String destinationCountry) { this.destinationCountry = destinationCountry; }
    
    public String getDestinationPostal() { return destinationPostal; }
    public void setDestinationPostal(String destinationPostal) { this.destinationPostal = destinationPostal; }
    
    public String getDestinationCity() { return destinationCity; }
    public void setDestinationCity(String destinationCity) { this.destinationCity = destinationCity; }
    
    public List<CargoItem> getCargoItems() { return cargoItems; }
    public void setCargoItems(List<CargoItem> cargoItems) { this.cargoItems = cargoItems; }
    
    public Double getTotalWeight() { return totalWeight; }
    public void setTotalWeight(Double totalWeight) { this.totalWeight = totalWeight; }
    
    public Double getTotalVolume() { return totalVolume; }
    public void setTotalVolume(Double totalVolume) { this.totalVolume = totalVolume; }
    
    public String getIncoterm() { return incoterm; }
    public void setIncoterm(String incoterm) { this.incoterm = incoterm; }
    
    public String getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(String paymentTerms) { this.paymentTerms = paymentTerms; }
    
    public Boolean getDangerousGoods() { return dangerousGoods; }
    public void setDangerousGoods(Boolean dangerousGoods) { this.dangerousGoods = dangerousGoods; }
    
    public Boolean getTemperatureControlled() { return temperatureControlled; }
    public void setTemperatureControlled(Boolean temperatureControlled) { this.temperatureControlled = temperatureControlled; }
    
    public Boolean getHighValue() { return highValue; }
    public void setHighValue(Boolean highValue) { this.highValue = highValue; }
    
    public String getSpecialInstructions() { return specialInstructions; }
    public void setSpecialInstructions(String specialInstructions) { this.specialInstructions = specialInstructions; }
    
    public String getTransportMode() { return transportMode; }
    public void setTransportMode(String transportMode) { this.transportMode = transportMode; }
    
    public LocalDate getPreferredPickupDate() { return preferredPickupDate; }
    public void setPreferredPickupDate(LocalDate preferredPickupDate) { this.preferredPickupDate = preferredPickupDate; }
    
    public LocalDate getRequiredDeliveryDate() { return requiredDeliveryDate; }
    public void setRequiredDeliveryDate(LocalDate requiredDeliveryDate) { this.requiredDeliveryDate = requiredDeliveryDate; }
    
    // Inner class for cargo items
    public static class CargoItem {
        private Integer pieces;
        private Double length; // cm
        private Double width;  // cm
        private Double height; // cm
        private Double weight; // kg
        private Double volume; // m³
        private String commodity;
        
        public CargoItem() {}
        
        // Getters and Setters
        public Integer getPieces() { return pieces; }
        public void setPieces(Integer pieces) { this.pieces = pieces; }
        
        public Double getLength() { return length; }
        public void setLength(Double length) { this.length = length; }
        
        public Double getWidth() { return width; }
        public void setWidth(Double width) { this.width = width; }
        
        public Double getHeight() { return height; }
        public void setHeight(Double height) { this.height = height; }
        
        public Double getWeight() { return weight; }
        public void setWeight(Double weight) { this.weight = weight; }
        
        public Double getVolume() { return volume; }
        public void setVolume(Double volume) { this.volume = volume; }
        
        public String getCommodity() { return commodity; }
        public void setCommodity(String commodity) { this.commodity = commodity; }
    }
}
