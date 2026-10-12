package com.taxpilot.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taxpilot.domain.user.User;
import com.taxpilot.dto.FilingPeriodRequest;
import com.taxpilot.dto.FilingPeriodResponse;
import com.taxpilot.dto.PreKraCheckResponse;
import com.taxpilot.service.TaxFilingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/businesses")
public class TaxFilingController {

    private final TaxFilingService taxFilingService;

    public TaxFilingController(TaxFilingService taxFilingService) {
        this.taxFilingService = taxFilingService;
    }

    @PostMapping("/{businessId}/filing-periods")
    public ResponseEntity<FilingPeriodResponse> createFilingPeriod(@AuthenticationPrincipal User user,
                                                                   @PathVariable UUID businessId,
                                                                   @Valid @RequestBody FilingPeriodRequest request) {
        return ResponseEntity.ok(taxFilingService.createFilingPeriod(user.getId(), businessId, request));
    }

    @GetMapping("/{businessId}/filing-periods")
    public ResponseEntity<List<FilingPeriodResponse>> listFilingPeriods(@AuthenticationPrincipal User user,
                                                                        @PathVariable UUID businessId) {
        return ResponseEntity.ok(taxFilingService.listFilingPeriods(user.getId(), businessId));
    }

    @GetMapping("/{businessId}/filing-periods/{periodId}")
    public ResponseEntity<FilingPeriodResponse> getFilingPeriod(@AuthenticationPrincipal User user,
                                                                @PathVariable UUID businessId,
                                                                @PathVariable UUID periodId) {
        return ResponseEntity.ok(taxFilingService.getFilingPeriod(user.getId(), businessId, periodId));
    }

    @GetMapping("/{businessId}/pre-kra-check")
    public ResponseEntity<PreKraCheckResponse> runPreKraCheck(@AuthenticationPrincipal User user,
                                                              @PathVariable UUID businessId) {
        return ResponseEntity.ok(taxFilingService.runPreKraCheck(user.getId(), businessId));
    }
}
