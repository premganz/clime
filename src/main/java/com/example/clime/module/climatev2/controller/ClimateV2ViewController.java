package com.example.clime.module.climatev2.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ClimateV2ViewController {
    
    @GetMapping("/climatev2")
    public String climateV2Page() {
        return "forward:/climatev2/index.html";
    }
}
