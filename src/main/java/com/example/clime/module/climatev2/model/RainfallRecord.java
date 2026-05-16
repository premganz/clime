package com.example.clime.module.climatev2.model;

public class RainfallRecord {
    private int year;
    private double jan;
    private double feb;
    private double mar;
    private double april;
    private double may;
    private double june;
    private double july;
    private double aug;
    private double sept;
    private double oct;
    private double nov;
    private double dec;
    private double total;

    // Default constructor
    public RainfallRecord() {}

    // Constructor
    public RainfallRecord(int year, double jan, double feb, double mar, double april, 
                         double may, double june, double july, double aug, double sept, 
                         double oct, double nov, double dec, double total) {
        this.year = year;
        this.jan = jan;
        this.feb = feb;
        this.mar = mar;
        this.april = april;
        this.may = may;
        this.june = june;
        this.july = july;
        this.aug = aug;
        this.sept = sept;
        this.oct = oct;
        this.nov = nov;
        this.dec = dec;
        this.total = total;
    }

    // Getters and Setters
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public double getJan() { return jan; }
    public void setJan(double jan) { this.jan = jan; }

    public double getFeb() { return feb; }
    public void setFeb(double feb) { this.feb = feb; }

    public double getMar() { return mar; }
    public void setMar(double mar) { this.mar = mar; }

    public double getApril() { return april; }
    public void setApril(double april) { this.april = april; }

    public double getMay() { return may; }
    public void setMay(double may) { this.may = may; }

    public double getJune() { return june; }
    public void setJune(double june) { this.june = june; }

    public double getJuly() { return july; }
    public void setJuly(double july) { this.july = july; }

    public double getAug() { return aug; }
    public void setAug(double aug) { this.aug = aug; }

    public double getSept() { return sept; }
    public void setSept(double sept) { this.sept = sept; }

    public double getOct() { return oct; }
    public void setOct(double oct) { this.oct = oct; }

    public double getNov() { return nov; }
    public void setNov(double nov) { this.nov = nov; }

    public double getDec() { return dec; }
    public void setDec(double dec) { this.dec = dec; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    // Utility method to get rainfall for a specific month (1-12)
    public double getRainfallForMonth(int month) {
        switch (month) {
            case 1: return jan;
            case 2: return feb;
            case 3: return mar;
            case 4: return april;
            case 5: return may;
            case 6: return june;
            case 7: return july;
            case 8: return aug;
            case 9: return sept;
            case 10: return oct;
            case 11: return nov;
            case 12: return dec;
            default: throw new IllegalArgumentException("Invalid month: " + month);
        }
    }

    // Get month name
    public static String getMonthName(int month) {
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", 
                          "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        if (month >= 1 && month <= 12) {
            return months[month - 1];
        }
        throw new IllegalArgumentException("Invalid month: " + month);
    }

    @Override
    public String toString() {
        return String.format("RainfallRecord{year=%d, total=%.2f}", year, total);
    }
}
