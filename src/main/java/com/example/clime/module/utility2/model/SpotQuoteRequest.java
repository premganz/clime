package com.example.clime.module.utility2.model;

import java.time.LocalDate;

public class SpotQuoteRequest {
    // Company Information
    private String country;
    private String companyName;
    private String vatNumber;
    private String subject;
    
    // Shipment Details
    private Integer numberOfPackages;
    private String packageType;
    private Double weightKg;
    private String dimensions; // Height x Length x Width cm
    private String commodity;
    private String departureAddress;
    private String deliveryAddress;
    private LocalDate pickupDate;
    private LocalDate deliveryDate;
    private String typeOfGoods;
    private String dangerousGoodsInfo;
    private String transportMode; // Air, Sea, Road, Courier, Rail
    
    // Contact Information
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String postalCode;
    private String city;
    private String street;
    
    // Additional details
    private String inquiryDetails;
    
    // Constructors
    public SpotQuoteRequest() {}
    
    // Getters and Setters
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    
    public String getVatNumber() { return vatNumber; }
    public void setVatNumber(String vatNumber) { this.vatNumber = vatNumber; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public Integer getNumberOfPackages() { return numberOfPackages; }
    public void setNumberOfPackages(Integer numberOfPackages) { this.numberOfPackages = numberOfPackages; }
    
    public String getPackageType() { return packageType; }
    public void setPackageType(String packageType) { this.packageType = packageType; }
    
    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }
    
    public String getDimensions() { return dimensions; }
    public void setDimensions(String dimensions) { this.dimensions = dimensions; }
    
    public String getCommodity() { return commodity; }
    public void setCommodity(String commodity) { this.commodity = commodity; }
    
    public String getDepartureAddress() { return departureAddress; }
    public void setDepartureAddress(String departureAddress) { this.departureAddress = departureAddress; }
    
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    
    public LocalDate getPickupDate() { return pickupDate; }
    public void setPickupDate(LocalDate pickupDate) { this.pickupDate = pickupDate; }
    
    public LocalDate getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(LocalDate deliveryDate) { this.deliveryDate = deliveryDate; }
    
    public String getTypeOfGoods() { return typeOfGoods; }
    public void setTypeOfGoods(String typeOfGoods) { this.typeOfGoods = typeOfGoods; }
    
    public String getDangerousGoodsInfo() { return dangerousGoodsInfo; }
    public void setDangerousGoodsInfo(String dangerousGoodsInfo) { this.dangerousGoodsInfo = dangerousGoodsInfo; }
    
    public String getTransportMode() { return transportMode; }
    public void setTransportMode(String transportMode) { this.transportMode = transportMode; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    
    public String getInquiryDetails() { return inquiryDetails; }
    public void setInquiryDetails(String inquiryDetails) { this.inquiryDetails = inquiryDetails; }
}
