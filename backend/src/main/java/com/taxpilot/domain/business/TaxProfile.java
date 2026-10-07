package com.taxpilot.domain.business;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tax_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "business_id", nullable = false, unique = true)
    private Business business;

    @Column(nullable = false)
    private String businessType;

    @Column(precision = 19, scale = 2)
    private BigDecimal annualTurnover;

    @Column(precision = 19, scale = 2)
    private BigDecimal monthlyTurnover;

    private Integer employeeCount;

    private boolean vatRegistered;

    private LocalDate vatRegistrationDate;

    private boolean etimsRegistered;

    private boolean hasEmployees;

    private boolean makesWithholdingPayments;

    private boolean receivesRentalIncome;

    private boolean importsGoods;

    private boolean exportsGoods;

    private boolean hasForeignTransactions;

    private boolean hasDigitalServices;

    private boolean hasExciseActivities;

    private boolean incomeTaxApplicable;

    private boolean corporationTaxApplicable;

    private boolean turnoverTaxApplicable;
}
