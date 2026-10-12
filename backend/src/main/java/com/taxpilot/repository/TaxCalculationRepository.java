package com.taxpilot.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.taxpilot.domain.tax.TaxCalculation;

public interface TaxCalculationRepository extends JpaRepository<TaxCalculation, UUID> {
    List<TaxCalculation> findByFilingPeriodIdOrderByTaxTypeAsc(UUID filingPeriodId);

    List<TaxCalculation> findByBusinessIdOrderByComputedAtDesc(UUID businessId);
}
