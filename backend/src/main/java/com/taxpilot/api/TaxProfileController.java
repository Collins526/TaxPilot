package com.taxpilot.api;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taxpilot.dto.TaxProfileRequest;
import com.taxpilot.dto.TaxProfileResponse;
import com.taxpilot.domain.user.User;
import com.taxpilot.service.TaxProfileService;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/businesses")
public class TaxProfileController {

    private final TaxProfileService taxProfileService;

    public TaxProfileController(TaxProfileService taxProfileService) {
        this.taxProfileService = taxProfileService;
    }

    @GetMapping("/{businessId}/tax-profile")
    public ResponseEntity<TaxProfileResponse> getTaxProfile(@AuthenticationPrincipal User user,
                                                            @PathVariable UUID businessId) {
        return ResponseEntity.ok(taxProfileService.getTaxProfile(user.getId(), businessId));
    }

    @PutMapping("/{businessId}/tax-profile")
    public ResponseEntity<TaxProfileResponse> updateTaxProfile(@AuthenticationPrincipal User user,
                                                             @PathVariable UUID businessId,
                                                             @Valid @RequestBody TaxProfileRequest request) {
        return ResponseEntity.ok(taxProfileService.saveTaxProfile(user.getId(), businessId, request));
    }
}
