package com.taxpilot.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.taxpilot.domain.tax.TaxRuleVersion;

public interface TaxRuleVersionRepository extends JpaRepository<TaxRuleVersion, UUID> {
    List<TaxRuleVersion> findByRuleCodeIn(List<String> ruleCodes);

    List<TaxRuleVersion> findAllByOrderByRuleCodeAscEffectiveFromDesc();

    java.util.Optional<TaxRuleVersion> findByRuleCodeAndRuleVersion(String ruleCode, String ruleVersion);
}
