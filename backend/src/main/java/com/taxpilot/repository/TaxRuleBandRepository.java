package com.taxpilot.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.taxpilot.domain.tax.TaxRuleBand;

public interface TaxRuleBandRepository extends JpaRepository<TaxRuleBand, UUID> {
    List<TaxRuleBand> findByRuleCodeAndRuleVersionOrderByBandOrderAsc(String ruleCode, String ruleVersion);

    @Modifying
    @Query("delete from TaxRuleBand band where band.ruleCode = :ruleCode and band.ruleVersion = :ruleVersion")
    void deleteByRuleCodeAndRuleVersion(@Param("ruleCode") String ruleCode, @Param("ruleVersion") String ruleVersion);
}
