package com.taxpilot.domain.tax;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.taxpilot.domain.business.Business;

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
@Table(name = "filing_periods")
@Getter
@Setter
@NoArgsConstructor
public class FilingPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "taxable_sales", precision = 19, scale = 2)
    private BigDecimal taxableSales;

    @Column(name = "input_vat", precision = 19, scale = 2)
    private BigDecimal inputVat;

    @Column(name = "payroll_gross", precision = 19, scale = 2)
    private BigDecimal payrollGross;

    @Column(name = "withholding_base", precision = 19, scale = 2)
    private BigDecimal withholdingBase;

    @Column(name = "taxable_profit", precision = 19, scale = 2)
    private BigDecimal taxableProfit;

    @Column(name = "etims_declared_sales", precision = 19, scale = 2)
    private BigDecimal etimsDeclaredSales;

    @Column(length = 512)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private UUID createdBy;
}
