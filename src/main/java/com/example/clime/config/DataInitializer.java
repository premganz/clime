package com.example.clime.config;

import com.example.clime.service.WeatherDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class DataInitializer implements CommandLineRunner {
    // Weather data initialization 
    @Autowired
    private WeatherDataService weatherDataService;
    
    @Override
    public void run(String... args) throws Exception {
        // Check if data file already exists
        File csvFile = new File("src/main/resources/scrambled_weather_data.csv");
        
        if (csvFile.exists() && csvFile.length() > 0) {
            System.out.println("Weather data already exists. Skipping data fetch.");
            System.out.println("File size: " + csvFile.length() + " bytes");
            System.out.println("Unscramble key: " + weatherDataService.getUnscrambleKey());
        } else {
            System.out.println("Weather data not found or empty. Fetching data from remote source...");
            System.out.println("This may take a few minutes...");
            try {
                weatherDataService.fetchAndProcessAllData();
                System.out.println("Data initialization complete!");
                System.out.println("Unscramble key: " + weatherDataService.getUnscrambleKey());
            } catch (Exception e) {
                System.err.println("Error during data fetch: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
