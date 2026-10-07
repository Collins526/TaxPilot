package com.taxpilot.domain.tax;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tax_rule_versions")
@Getter
@Setter
@NoArgsConstructor
public class TaxRuleVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "rule_code", nullable = false, length = 64)
    private String ruleCode;

    @Column(name = "rule_version", nullable = false, length = 64)
    private String ruleVersion;

    @Column(nullable = false, length = 32)
    private String frequency;

    @Column(name = "due_day")
    private Integer dueDay;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(precision = 19, scale = 6)
    private BigDecimal rate;

    @Column(precision = 19, scale = 2)
    private BigDecimal threshold;

    @Column(nullable = false, length = 512)
    private String source;
}
