package com.example.neft.controller;

import com.example.neft.service.NeftPaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@DirtiesContext
class NeftPaymentControllerTest {

    @Autowired private WebApplicationContext wac;
    @Autowired private NeftPaymentService service;

    private MockMvc mvc() { return MockMvcBuilders.webAppContextSetup(wac).build(); }

    @Test
    void listPageRenders() throws Exception {
        mvc().perform(get("/payments"))
             .andExpect(status().isOk())
             .andExpect(view().name("payments/list"))
             .andExpect(model().attributeExists("payments"));
    }

    @Test
    void newFormRenders() throws Exception {
        mvc().perform(get("/payments/new"))
             .andExpect(status().isOk())
             .andExpect(view().name("payments/schedule"))
             .andExpect(model().attributeExists("payment"));
    }

    @Test
    void scheduleValidPaymentRedirects() throws Exception {
        mvc().perform(post("/payments/schedule")
                .param("remitterName", "Alice")
                .param("remitterAccount", "123456789012")
                .param("beneficiaryName", "Bob")
                .param("beneficiaryAccount", "987654321098")
                .param("ifsc", "HDFC0001234")
                .param("amount", "1500.00")
                .param("valueDate", LocalDate.now().toString())
                .param("purpose", "Invoice"))
             .andExpect(status().is3xxRedirection())
             .andExpect(redirectedUrl("/payments"));
    }

    @Test
    void invalidIfscFailsValidationAndReturnsForm() throws Exception {
        mvc().perform(post("/payments/schedule")
                .param("remitterName", "Alice")
                .param("remitterAccount", "123456789012")
                .param("beneficiaryName", "Bob")
                .param("beneficiaryAccount", "987654321098")
                .param("ifsc", "BAD_IFSC")
                .param("amount", "1500.00")
                .param("valueDate", LocalDate.now().toString())
                .param("purpose", "Invoice"))
             .andExpect(status().isOk())
             .andExpect(view().name("payments/schedule"))
             .andExpect(model().attributeHasFieldErrors("payment", "ifsc"));
    }

    @Test
    void pastValueDateIsRejected() throws Exception {
        mvc().perform(post("/payments/schedule")
                .param("remitterName", "Alice")
                .param("remitterAccount", "123456789012")
                .param("beneficiaryName", "Bob")
                .param("beneficiaryAccount", "987654321098")
                .param("ifsc", "HDFC0001234")
                .param("amount", "1500.00")
                .param("valueDate", LocalDate.now().minusDays(1).toString())
                .param("purpose", "Invoice"))
             .andExpect(status().isOk())
             .andExpect(model().attributeHasFieldErrors("payment", "valueDate"));
    }
}
