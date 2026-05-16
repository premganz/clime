package com.example.neft.model;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * NEFT (National Electronic Funds Transfer) payment instruction.
 * Field shapes follow RBI NEFT message conventions (NEFT R-41/N-06 inspired).
 */
public class NeftPayment {

    public enum Status { SCHEDULED, BATCHED, SETTLED, REJECTED }

    private Long id;

    // ----- Remitter -----
    @NotBlank(message = "Remitter name is required")
    @Size(max = 60)
    private String remitterName;

    @NotBlank(message = "Remitter account is required")
    @Pattern(regexp = "\\d{9,18}", message = "Account number must be 9-18 digits")
    private String remitterAccount;

    // ----- Beneficiary -----
    @NotBlank(message = "Beneficiary name is required")
    @Size(max = 60)
    private String beneficiaryName;

    @NotBlank(message = "Beneficiary account is required")
    @Pattern(regexp = "\\d{9,18}", message = "Account number must be 9-18 digits")
    private String beneficiaryAccount;

    /** IFSC: 4 letters + '0' + 6 alphanumerics (e.g. HDFC0001234). */
    @NotBlank(message = "IFSC is required")
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$",
             message = "IFSC must match the format AAAA0XXXXXX (e.g. HDFC0001234)")
    private String ifsc;

    // ----- Amount -----
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Minimum NEFT amount is ₹1.00")
    @DecimalMax(value = "10000000.00", message = "Per-instruction cap in this demo is ₹1,00,00,000")
    private BigDecimal amount;

    // ----- Scheduling -----
    @NotNull(message = "Value date is required")
    private LocalDate valueDate;

    @NotBlank(message = "Purpose is required")
    @Size(max = 80)
    private String purpose;

    // ----- Advice -----
    @Email(message = "Invalid email")
    private String adviceEmail;

    @Pattern(regexp = "^$|^[6-9]\\d{9}$",
             message = "Mobile must be a 10-digit Indian number")
    private String adviceMobile;

    // ----- System-assigned -----
    private String utr;             // Unique Transaction Reference
    private String batchSlot;       // e.g. "B07 13:00"
    private Status status;
    private LocalDateTime createdAt;
    private String rejectReason;

    public NeftPayment() {}

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRemitterName() { return remitterName; }
    public void setRemitterName(String v) { this.remitterName = v; }
    public String getRemitterAccount() { return remitterAccount; }
    public void setRemitterAccount(String v) { this.remitterAccount = v; }
    public String getBeneficiaryName() { return beneficiaryName; }
    public void setBeneficiaryName(String v) { this.beneficiaryName = v; }
    public String getBeneficiaryAccount() { return beneficiaryAccount; }
    public void setBeneficiaryAccount(String v) { this.beneficiaryAccount = v; }
    public String getIfsc() { return ifsc; }
    public void setIfsc(String v) { this.ifsc = v == null ? null : v.toUpperCase(); }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal v) { this.amount = v; }
    public LocalDate getValueDate() { return valueDate; }
    public void setValueDate(LocalDate v) { this.valueDate = v; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String v) { this.purpose = v; }
    public String getAdviceEmail() { return adviceEmail; }
    public void setAdviceEmail(String v) { this.adviceEmail = v; }
    public String getAdviceMobile() { return adviceMobile; }
    public void setAdviceMobile(String v) { this.adviceMobile = v; }
    public String getUtr() { return utr; }
    public void setUtr(String v) { this.utr = v; }
    public String getBatchSlot() { return batchSlot; }
    public void setBatchSlot(String v) { this.batchSlot = v; }
    public Status getStatus() { return status; }
    public void setStatus(Status v) { this.status = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String v) { this.rejectReason = v; }
}
