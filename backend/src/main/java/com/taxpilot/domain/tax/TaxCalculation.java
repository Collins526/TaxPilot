package com.taxpilot.domain.tax;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tax_calculations")
@Getter
@Setter
@NoArgsConstructor
public class TaxCalculation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "filing_period_id", nullable = false)
    private FilingPeriod filingPeriod;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "tax_type", nullable = false, length = 64)
    private String taxType;

    @Column(name = "rule_code", nullable = false, length = 64)
    private String ruleCode;

    @Column(name = "rule_version", nullable = false, length = 64)
    private String ruleVersion;

    @Column(precision = 9, scale = 6)
    private BigDecimal rate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 512)
    private String basis;

    @Column(name = "computed_at", nullable = false)
    private LocalDateTime computedAt;
}
