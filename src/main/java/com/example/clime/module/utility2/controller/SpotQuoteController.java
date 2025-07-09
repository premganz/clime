package com.example.clime.module.utility2.controller;

import com.example.clime.module.utility2.model.SpotQuoteRequest;
import com.example.clime.module.utility2.model.SpotQuoteResponse;
import com.example.clime.module.utility2.model.AirFreightQuoteRequest;
import com.example.clime.module.utility2.model.AirFreightQuoteResponse;
import com.example.clime.module.utility2.service.SpotQuoteService;
import com.example.clime.module.utility2.service.AirFreightQuoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/utility2")
public class SpotQuoteController {
    
    @Autowired
    private SpotQuoteService spotQuoteService;
    
    @Autowired
    @Qualifier("airFreightQuoteService")
    private AirFreightQuoteService airFreightQuoteService;
    
    // Store quotes in memory (in real app, use database)
    private final Map<String, SpotQuoteResponse> quotesDatabase = new HashMap<>();
    private final Map<String, AirFreightQuoteResponse> airFreightQuotesDatabase = new HashMap<>();

    @PostMapping("/spot-quote")
    public ResponseEntity<SpotQuoteResponse> createSpotQuote(@RequestBody SpotQuoteRequest request) {
        try {
            SpotQuoteResponse quote = spotQuoteService.calculateQuote(request);
            quotesDatabase.put(quote.getQuoteId(), quote);
            return ResponseEntity.ok(quote);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/air-freight-quote")
    public ResponseEntity<AirFreightQuoteResponse> createAirFreightQuote(@RequestBody AirFreightQuoteRequest request) {
        try {
            AirFreightQuoteResponse quote = airFreightQuoteService.calculateAirFreightQuote(request);
            airFreightQuotesDatabase.put(quote.getQuoteId(), quote);
            return ResponseEntity.ok(quote);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/spot-quote/{quoteId}")
    public ResponseEntity<SpotQuoteResponse> getQuote(@PathVariable String quoteId) {
        SpotQuoteResponse quote = quotesDatabase.get(quoteId);
        if (quote != null) {
            return ResponseEntity.ok(quote);
        }
        return ResponseEntity.notFound().build();
    }
    
    @GetMapping("/air-freight-quote/{quoteId}")
    public ResponseEntity<AirFreightQuoteResponse> getAirFreightQuote(@PathVariable String quoteId) {
        AirFreightQuoteResponse quote = airFreightQuotesDatabase.get(quoteId);
        if (quote != null) {
            return ResponseEntity.ok(quote);
        }
        return ResponseEntity.notFound().build();
    }
    
    @GetMapping("/spot-quotes")
    public ResponseEntity<List<SpotQuoteResponse>> getAllQuotes() {
        return ResponseEntity.ok(new ArrayList<>(quotesDatabase.values()));
    }
    
    @GetMapping("/air-freight-quotes")
    public ResponseEntity<List<AirFreightQuoteResponse>> getAllAirFreightQuotes() {
        return ResponseEntity.ok(new ArrayList<>(airFreightQuotesDatabase.values()));
    }
    
    @GetMapping("/transport-modes")
    public ResponseEntity<List<Map<String, String>>> getTransportModes() {
        List<Map<String, String>> modes = new ArrayList<>();
        
        modes.add(createModeMap("AIR", "Air Freight", "Fast delivery via airlines"));
        modes.add(createModeMap("SEA", "Sea Freight", "Cost-effective ocean shipping"));
        modes.add(createModeMap("ROAD", "Road Transport", "Flexible truck delivery"));
        modes.add(createModeMap("COURIER", "Courier Service", "Express door-to-door delivery"));
        modes.add(createModeMap("RAIL", "Rail Transport", "Eco-friendly rail shipping"));
        
        return ResponseEntity.ok(modes);
    }
    
    @GetMapping("/countries")
    public ResponseEntity<List<String>> getCountries() {
        List<String> countries = new ArrayList<>();
        countries.add("India");
        countries.add("United States");
        countries.add("United Kingdom");
        countries.add("Germany");
        countries.add("France");
        countries.add("China");
        countries.add("Japan");
        countries.add("Australia");
        countries.add("Canada");
        countries.add("Netherlands");
        
        return ResponseEntity.ok(countries);
    }
    
    private Map<String, String> createModeMap(String value, String label, String description) {
        Map<String, String> mode = new HashMap<>();
        mode.put("value", value);
        mode.put("label", label);
        mode.put("description", description);
        return mode;
    }
}
