package com.taxpilot.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taxpilot.domain.user.User;
import com.taxpilot.dto.BusinessResponse;
import com.taxpilot.dto.CreateBusinessRequest;
import com.taxpilot.dto.DashboardSummaryResponse;
import com.taxpilot.dto.TaxCalendarEntry;
import com.taxpilot.service.BusinessService;
import com.taxpilot.tax.TaxHealthScore;
import com.taxpilot.tax.TaxObligation;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/businesses")
public class BusinessController {

    private final BusinessService businessService;

    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }

    @PostMapping
    public ResponseEntity<BusinessResponse> createBusiness(@AuthenticationPrincipal User user,
                                                          @Valid @RequestBody CreateBusinessRequest request) {
        return ResponseEntity.ok(businessService.createBusiness(user.getId(), request));
    }

    @GetMapping
    public ResponseEntity<List<BusinessResponse>> listBusinesses(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(businessService.listBusinessesForUser(user.getId()));
    }

    @GetMapping("/{businessId}")
    public ResponseEntity<BusinessResponse> getBusiness(@AuthenticationPrincipal User user,
                                                      @PathVariable UUID businessId) {
        return ResponseEntity.ok(businessService.getBusinessForUser(user.getId(), businessId));
    }

    @PutMapping("/{businessId}")
    public ResponseEntity<BusinessResponse> updateBusiness(@AuthenticationPrincipal User user,
                                                           @PathVariable UUID businessId,
                                                           @Valid @RequestBody CreateBusinessRequest request) {
        return ResponseEntity.ok(businessService.updateBusiness(user.getId(), businessId, request));
    }

    @GetMapping("/{businessId}/obligations")
    public ResponseEntity<List<TaxObligation>> getObligations(@AuthenticationPrincipal User user,
                                                             @PathVariable UUID businessId) {
        return ResponseEntity.ok(businessService.getObligations(user.getId(), businessId));
    }

    @GetMapping("/{businessId}/calendar")
    public ResponseEntity<List<TaxCalendarEntry>> getCalendar(@AuthenticationPrincipal User user,
                                                            @PathVariable UUID businessId) {
        return ResponseEntity.ok(businessService.getCalendar(user.getId(), businessId));
    }

    @GetMapping("/{businessId}/tax-health")
    public ResponseEntity<TaxHealthScore> getTaxHealth(@AuthenticationPrincipal User user,
                                                     @PathVariable UUID businessId) {
        return ResponseEntity.ok(businessService.getTaxHealth(user.getId(), businessId));
    }

    @GetMapping("/{businessId}/dashboard")
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary(@AuthenticationPrincipal User user,
                                                                        @PathVariable UUID businessId) {
        return ResponseEntity.ok(businessService.getDashboardSummary(user.getId(), businessId));
    }
}
