package com.taxpilot.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.taxpilot.domain.tax.TaxRuleBand;

public interface TaxRuleBandRepository extends JpaRepository<TaxRuleBand, UUID> {
    List<TaxRuleBand> findByRuleCodeAndRuleVersionOrderByBandOrderAsc(String ruleCode, String ruleVersion);
}
