package com.taxpilot.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taxpilot.domain.user.User;
import com.taxpilot.dto.BusinessMemberInviteRequest;
import com.taxpilot.dto.BusinessMemberResponse;
import com.taxpilot.service.BusinessMemberService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/businesses/{businessId}/members")
public class BusinessMemberController {

    private final BusinessMemberService businessMemberService;

    public BusinessMemberController(BusinessMemberService businessMemberService) {
        this.businessMemberService = businessMemberService;
    }

    @GetMapping
    public ResponseEntity<List<BusinessMemberResponse>> listMembers(@AuthenticationPrincipal User user,
                                                                    @PathVariable UUID businessId) {
        return ResponseEntity.ok(businessMemberService.listMembers(user.getId(), businessId));
    }

    @PostMapping
    public ResponseEntity<BusinessMemberResponse> addMember(@AuthenticationPrincipal User user,
                                                            @PathVariable UUID businessId,
                                                            @Valid @RequestBody BusinessMemberInviteRequest request) {
        return ResponseEntity.ok(businessMemberService.addMember(user.getId(), businessId, request));
    }

    @DeleteMapping("/{memberUserId}")
    public ResponseEntity<Void> removeMember(@AuthenticationPrincipal User user,
                                             @PathVariable UUID businessId,
                                             @PathVariable UUID memberUserId) {
        businessMemberService.removeMember(user.getId(), businessId, memberUserId);
        return ResponseEntity.noContent().build();
    }
}
