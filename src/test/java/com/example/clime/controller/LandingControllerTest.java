package com.example.clime.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LandingController.class)
class LandingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testLandingEndpoint() throws Exception {
        mockMvc.perform(get("/api/landing"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/plain;charset=UTF-8"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("BACKEND WORKING!")));
    }

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/plain;charset=UTF-8"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Health Check OK")));
    }
}
