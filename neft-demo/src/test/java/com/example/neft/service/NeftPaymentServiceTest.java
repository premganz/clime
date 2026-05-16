package com.example.neft.service;

import com.example.neft.model.NeftPayment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class NeftPaymentServiceTest {

    private final NeftPaymentService service = new NeftPaymentService();

    private NeftPayment sample() {
        NeftPayment p = new NeftPayment();
        p.setRemitterName("Alice");
        p.setRemitterAccount("123456789012");
        p.setBeneficiaryName("Bob");
        p.setBeneficiaryAccount("987654321098");
        p.setIfsc("HDFC0001234");
        p.setAmount(new BigDecimal("1500.00"));
        p.setValueDate(LocalDate.now());
        p.setPurpose("Invoice");
        return p;
    }

    @Test
    void scheduleAssignsIdUtrBatchAndStatus() {
        NeftPayment saved = service.schedule(sample());

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertEquals(NeftPayment.Status.SCHEDULED, saved.getStatus());

        // UTR: DEMON + 5 digits (yy+ddd) + 9-digit seq = 17 chars (DEMON has 5)
        assertNotNull(saved.getUtr());
        assertTrue(saved.getUtr().startsWith("DEMON"), "UTR should start with DEMON");
        assertEquals(5 + 2 + 3 + 9, saved.getUtr().length());

        // Batch slot like "B07 13:00"
        assertTrue(saved.getBatchSlot().matches("B\\d{2} \\d{2}:\\d{2}"),
                "Batch slot format unexpected: " + saved.getBatchSlot());
    }

    @Test
    void futureValueDateGetsFirstSlotOfThatDay() {
        NeftPayment p = sample();
        p.setValueDate(LocalDate.now().plusDays(2));
        NeftPayment saved = service.schedule(p);
        assertEquals("B01 00:30", saved.getBatchSlot());
    }

    @Test
    void cancelOnlyScheduledMovesToRejected() {
        NeftPayment saved = service.schedule(sample());
        assertTrue(service.cancel(saved.getId()));
        assertEquals(NeftPayment.Status.REJECTED, saved.getStatus());
        assertEquals("Cancelled by user", saved.getRejectReason());

        // second cancel is a no-op (status is no longer SCHEDULED)
        assertFalse(service.cancel(saved.getId()));
    }

    @Test
    void cancelUnknownIdReturnsFalse() {
        assertFalse(service.cancel(99999L));
    }

    @Test
    void settlementSweepPromotesDueScheduledItems() {
        NeftPayment dueToday = service.schedule(sample());
        NeftPayment future = sample();
        future.setValueDate(LocalDate.now().plusDays(1));
        NeftPayment notDue = service.schedule(future);

        int moved = service.runSettlementSweep();

        assertEquals(1, moved);
        assertEquals(NeftPayment.Status.SETTLED, dueToday.getStatus());
        assertEquals(NeftPayment.Status.SCHEDULED, notDue.getStatus());
    }

    @Test
    void findAllReturnsNewestFirst() {
        NeftPayment a = service.schedule(sample());
        NeftPayment b = service.schedule(sample());
        assertEquals(b.getId(), service.findAll().get(0).getId());
        assertEquals(a.getId(), service.findAll().get(1).getId());
    }
}
