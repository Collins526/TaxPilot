package com.taxpilot.domain.tax;

import java.math.BigDecimal;
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
@Table(name = "tax_rule_bands")
@Getter
@Setter
@NoArgsConstructor
public class TaxRuleBand {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "rule_code", nullable = false, length = 64)
    private String ruleCode;

    @Column(name = "rule_version", nullable = false, length = 64)
    private String ruleVersion;

    @Column(name = "band_order", nullable = false)
    private int bandOrder;

    @Column(name = "lower_bound", nullable = false, precision = 19, scale = 2)
    private BigDecimal lowerBound;

    @Column(name = "upper_bound", precision = 19, scale = 2)
    private BigDecimal upperBound;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal rate;
}
