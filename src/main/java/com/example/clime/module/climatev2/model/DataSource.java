package com.example.clime.module.climatev2.model;

public enum DataSource {
    CSV("CSV Data", "Historical CSV rainfall data (1901-2021)"),
    KWS("KWS Data", "KWS Chennai website data (2000-2025)");
    
    private final String displayName;
    private final String description;
    
    DataSource(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}