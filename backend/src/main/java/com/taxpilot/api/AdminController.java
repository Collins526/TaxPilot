package com.taxpilot.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.taxpilot.domain.user.User;
import com.taxpilot.dto.AdminDtos.AuditView;
import com.taxpilot.dto.AdminDtos.BusinessView;
import com.taxpilot.dto.AdminDtos.EnabledRequest;
import com.taxpilot.dto.AdminDtos.MembershipRequest;
import com.taxpilot.dto.AdminDtos.MembershipView;
import com.taxpilot.dto.AdminDtos.Overview;
import com.taxpilot.dto.AdminDtos.TaxRuleRequest;
import com.taxpilot.dto.AdminDtos.TaxRuleView;
import com.taxpilot.dto.AdminDtos.UserView;
import com.taxpilot.dto.CreateBusinessRequest;
import com.taxpilot.service.AdminService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/overview")
    public Overview overview() {
        return adminService.overview();
    }

    @GetMapping("/users")
    public List<UserView> users(@RequestParam(defaultValue = "200") int limit) {
        return adminService.users(limit);
    }

    @PatchMapping("/users/{userId}/enabled")
    public UserView setUserEnabled(
            @PathVariable UUID userId,
            @Valid @RequestBody EnabledRequest request,
            @AuthenticationPrincipal User actor) {
        return adminService.setUserEnabled(userId, request.enabled(), actor.getId());
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable UUID userId, @AuthenticationPrincipal User actor) {
        adminService.deleteUser(userId, actor.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/businesses")
    public List<BusinessView> businesses(@RequestParam(defaultValue = "200") int limit) {
        return adminService.businesses(limit);
    }

    @PutMapping("/businesses/{businessId}")
    public BusinessView updateBusiness(
            @PathVariable UUID businessId, @Valid @RequestBody CreateBusinessRequest request) {
        return adminService.updateBusiness(businessId, request);
    }

    @DeleteMapping("/businesses/{businessId}")
    public ResponseEntity<Void> deleteBusiness(@PathVariable UUID businessId) {
        adminService.deleteBusiness(businessId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/businesses/{businessId}/members")
    public List<MembershipView> addMembership(
            @PathVariable UUID businessId, @Valid @RequestBody MembershipRequest request) {
        return adminService.addMembership(businessId, request);
    }

    @DeleteMapping("/businesses/{businessId}/members/{userId}")
    public List<MembershipView> removeMembership(
            @PathVariable UUID businessId, @PathVariable UUID userId) {
        return adminService.removeMembership(businessId, userId);
    }

    @GetMapping("/audit-logs")
    public List<AuditView> auditLogs(@RequestParam(defaultValue = "100") int limit) {
        return adminService.auditLogs(limit);
    }

    @GetMapping("/tax-rules")
    public List<TaxRuleView> taxRules() {
        return adminService.taxRules();
    }

    @PostMapping("/tax-rules")
    public TaxRuleView createTaxRule(@Valid @RequestBody TaxRuleRequest request) {
        return adminService.createTaxRule(request);
    }

    @PutMapping("/tax-rules/{ruleId}")
    public TaxRuleView updateTaxRule(
            @PathVariable UUID ruleId, @Valid @RequestBody TaxRuleRequest request) {
        return adminService.updateTaxRule(ruleId, request);
    }
}
