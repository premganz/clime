package com.example.clime.model;

public class WeatherRecord {
    private String year;
    private String month;
    private String day;
    private String meanTemp;
    private String highTemp;
    private String highTime;
    private String lowTemp;
    private String lowTime;
    private String heatDegDays;
    private String coolDegDays;
    private String rain;
    private String windAvg;
    private String windHi;
    private String windHiTime;
    private String domDir;
    private String meanBarom;
    private String meanHum;
    private String flagged;
    private String anomalyNote;

    // Default constructor
    public WeatherRecord() {}

    // Constructor
    public WeatherRecord(String year, String month, String day, String meanTemp, String highTemp, 
                        String highTime, String lowTemp, String lowTime, String heatDegDays, 
                        String coolDegDays, String rain, String windAvg, String windHi, 
                        String windHiTime, String domDir, String meanBarom, String meanHum) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.meanTemp = meanTemp;
        this.highTemp = highTemp;
        this.highTime = highTime;
        this.lowTemp = lowTemp;
        this.lowTime = lowTime;
        this.heatDegDays = heatDegDays;
        this.coolDegDays = coolDegDays;
        this.rain = rain;
        this.windAvg = windAvg;
        this.windHi = windHi;
        this.windHiTime = windHiTime;
        this.domDir = domDir;
        this.meanBarom = meanBarom;
        this.meanHum = meanHum;
        this.flagged = "F";
        this.anomalyNote = "";
    }

    // Getters and Setters
    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }

    public String getMeanTemp() { return meanTemp; }
    public void setMeanTemp(String meanTemp) { this.meanTemp = meanTemp; }

    public String getHighTemp() { return highTemp; }
    public void setHighTemp(String highTemp) { this.highTemp = highTemp; }

    public String getHighTime() { return highTime; }
    public void setHighTime(String highTime) { this.highTime = highTime; }

    public String getLowTemp() { return lowTemp; }
    public void setLowTemp(String lowTemp) { this.lowTemp = lowTemp; }

    public String getLowTime() { return lowTime; }
    public void setLowTime(String lowTime) { this.lowTime = lowTime; }

    public String getHeatDegDays() { return heatDegDays; }
    public void setHeatDegDays(String heatDegDays) { this.heatDegDays = heatDegDays; }

    public String getCoolDegDays() { return coolDegDays; }
    public void setCoolDegDays(String coolDegDays) { this.coolDegDays = coolDegDays; }

    public String getRain() { return rain; }
    public void setRain(String rain) { this.rain = rain; }

    public String getWindAvg() { return windAvg; }
    public void setWindAvg(String windAvg) { this.windAvg = windAvg; }

    public String getWindHi() { return windHi; }
    public void setWindHi(String windHi) { this.windHi = windHi; }

    public String getWindHiTime() { return windHiTime; }
    public void setWindHiTime(String windHiTime) { this.windHiTime = windHiTime; }

    public String getDomDir() { return domDir; }
    public void setDomDir(String domDir) { this.domDir = domDir; }

    public String getMeanBarom() { return meanBarom; }
    public void setMeanBarom(String meanBarom) { this.meanBarom = meanBarom; }

    public String getMeanHum() { return meanHum; }
    public void setMeanHum(String meanHum) { this.meanHum = meanHum; }

    public String getFlagged() { return flagged; }
    public void setFlagged(String flagged) { this.flagged = flagged; }

    public String getAnomalyNote() { return anomalyNote; }
    public void setAnomalyNote(String anomalyNote) { this.anomalyNote = anomalyNote; }
}
