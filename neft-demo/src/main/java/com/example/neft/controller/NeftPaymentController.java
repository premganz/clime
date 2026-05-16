package com.example.neft.controller;

import com.example.neft.model.NeftPayment;
import com.example.neft.service.NeftPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.time.LocalDate;

@Controller
@RequestMapping("/payments")
public class NeftPaymentController {

    private final NeftPaymentService service;

    @Autowired
    public NeftPaymentController(NeftPaymentService service) {
        this.service = service;
    }

    @GetMapping({"", "/"})
    public String list(Model model) {
        model.addAttribute("payments", service.findAll());
        return "payments/list";
    }

    @GetMapping("/new")
    public String form(Model model) {
        if (!model.containsAttribute("payment")) {
            NeftPayment p = new NeftPayment();
            p.setValueDate(LocalDate.now());
            model.addAttribute("payment", p);
        }
        return "payments/schedule";
    }

    @PostMapping("/schedule")
    public String schedule(@Valid @org.springframework.web.bind.annotation.ModelAttribute("payment") NeftPayment payment,
                           BindingResult br,
                           RedirectAttributes ra) {
        if (payment.getValueDate() != null && payment.getValueDate().isBefore(LocalDate.now())) {
            br.rejectValue("valueDate", "valueDate.past", "Value date cannot be in the past");
        }
        if (br.hasErrors()) {
            return "payments/schedule";
        }
        NeftPayment saved = service.schedule(payment);
        ra.addFlashAttribute("flash",
                "Payment scheduled. UTR " + saved.getUtr() + " in batch " + saved.getBatchSlot());
        return "redirect:/payments";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes ra) {
        boolean ok = service.cancel(id);
        ra.addFlashAttribute("flash", ok ? "Payment cancelled." : "Cannot cancel (not scheduled).");
        return "redirect:/payments";
    }

    @PostMapping("/settle")
    public String settle(RedirectAttributes ra) {
        int n = service.runSettlementSweep();
        ra.addFlashAttribute("flash", "Settlement sweep moved " + n + " payment(s) to SETTLED.");
        return "redirect:/payments";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        NeftPayment p = service.findById(id);
        if (p == null) return "redirect:/payments";
        model.addAttribute("payment", p);
        return "payments/detail";
    }
}
