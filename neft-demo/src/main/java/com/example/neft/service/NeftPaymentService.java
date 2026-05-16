package com.example.neft.service;

import com.example.neft.model.NeftPayment;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory NEFT scheduler.
 *
 * NEFT settles in half-hourly batches. Since 16-Dec-2019 NEFT is 24x7,
 * with 48 half-hour batches per day (B01 at 00:30 ... B48 at 24:00).
 * For this demo we round each instruction up to the next half-hour slot
 * on the chosen value date.
 */
@Service
public class NeftPaymentService {

    private final Map<Long, NeftPayment> store = new ConcurrentHashMap<>();
    private final AtomicLong idSeq = new AtomicLong(1000);
    private final AtomicLong utrSeq = new AtomicLong(1);

    public NeftPayment schedule(NeftPayment p) {
        long id = idSeq.incrementAndGet();
        p.setId(id);
        p.setCreatedAt(LocalDateTime.now());
        p.setUtr(generateUtr(id));
        p.setBatchSlot(assignBatch(p.getValueDate()));
        p.setStatus(NeftPayment.Status.SCHEDULED);
        store.put(id, p);
        return p;
    }

    public List<NeftPayment> findAll() {
        List<NeftPayment> list = new ArrayList<>(store.values());
        list.sort(Comparator.comparing(NeftPayment::getCreatedAt)
                .thenComparing(NeftPayment::getId)
                .reversed());
        return list;
    }

    public NeftPayment findById(Long id) {
        return store.get(id);
    }

    public boolean cancel(Long id) {
        NeftPayment p = store.get(id);
        if (p == null) return false;
        if (p.getStatus() != NeftPayment.Status.SCHEDULED) return false;
        p.setStatus(NeftPayment.Status.REJECTED);
        p.setRejectReason("Cancelled by user");
        return true;
    }

    /** Mark all scheduled items whose batch time has passed as SETTLED. */
    public int runSettlementSweep() {
        int n = 0;
        LocalDateTime now = LocalDateTime.now();
        for (NeftPayment p : store.values()) {
            if (p.getStatus() == NeftPayment.Status.SCHEDULED
                    && !p.getValueDate().isAfter(now.toLocalDate())) {
                p.setStatus(NeftPayment.Status.SETTLED);
                n++;
            }
        }
        return n;
    }

    // ----- helpers -----

    private String generateUtr(long id) {
        // UTR format: BANK + N + YYDDD + 9-digit sequence  (16 chars, RBI-style)
        LocalDate today = LocalDate.now();
        String yy = String.format("%02d", today.getYear() % 100);
        String ddd = String.format("%03d", today.getDayOfYear());
        String seq = String.format("%09d", utrSeq.getAndIncrement());
        return "DEMON" + yy + ddd + seq;
    }

    private String assignBatch(LocalDate valueDate) {
        LocalTime base;
        if (valueDate.isAfter(LocalDate.now())) {
            base = LocalTime.of(0, 30);            // first slot of value date
        } else {
            LocalTime now = LocalTime.now();
            int minutes = now.getHour() * 60 + now.getMinute();
            int next = ((minutes / 30) + 1) * 30;  // round up to next half hour
            if (next >= 24 * 60) next = 24 * 60;
            base = LocalTime.of((next / 60) % 24, next % 60);
        }
        int batchNo = (base.getHour() * 60 + base.getMinute()) / 30;
        if (batchNo == 0) batchNo = 48;
        return String.format("B%02d %02d:%02d", batchNo, base.getHour(), base.getMinute());
    }
}
