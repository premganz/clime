package com.example.clime.module.utility.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
@CrossOrigin(origins = "*")
public class UtilityController {
    @GetMapping("/api/utility/hello")
    public String hello() {
        return "Hello from Utility Module!";
    }
}
