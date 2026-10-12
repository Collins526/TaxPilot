package com.taxpilot.api;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taxpilot.domain.audit.AuditAction;
import com.taxpilot.domain.user.User;
import com.taxpilot.dto.AssistantRequest;
import com.taxpilot.dto.AssistantResponse;
import com.taxpilot.dto.BusinessResponse;
import com.taxpilot.service.AiAssistantService;
import com.taxpilot.service.AuditService;
import com.taxpilot.service.BusinessService;
import com.taxpilot.tax.TaxObligation;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/businesses")
public class AssistantController {

    private final AiAssistantService aiAssistantService;
    private final BusinessService businessService;
    private final AuditService auditService;

    public AssistantController(AiAssistantService aiAssistantService,
                               BusinessService businessService,
                               AuditService auditService) {
        this.aiAssistantService = aiAssistantService;
        this.businessService = businessService;
        this.auditService = auditService;
    }

    @PostMapping("/{businessId}/assistant")
    public ResponseEntity<AssistantResponse> ask(@AuthenticationPrincipal User user,
                                                 @PathVariable UUID businessId,
                                                 @Valid @RequestBody AssistantRequest request) {
        BusinessResponse business = businessService.getBusinessForUser(user.getId(), businessId);
        String context = buildContext(business, businessService.getObligations(user.getId(), businessId));

        String answer = aiAssistantService.ask(context, request.message());
        auditService.record(AuditAction.ASSISTANT_QUERY, "ASSISTANT", businessId, businessId,
                "Assistant question asked: " + truncate(request.message()));

        return ResponseEntity.ok(new AssistantResponse(answer, aiAssistantService.disclaimer(), aiAssistantService.model()));
    }

    private String buildContext(BusinessResponse business, java.util.List<TaxObligation> obligations) {
        StringBuilder context = new StringBuilder();
        context.append("Business name: ").append(business.name()).append('\n');
        context.append("Business type: ").append(business.businessType()).append('\n');
        context.append("VAT registered: ").append(business.vatRegistered()).append('\n');
        context.append("eTIMS registered: ").append(business.etimsRegistered()).append('\n');
        context.append("Applicable obligations (from the deterministic engine):\n");
        if (obligations.isEmpty()) {
            context.append("- none currently indicated by the saved profile\n");
        } else {
            for (TaxObligation obligation : obligations) {
                context.append("- ").append(obligation.taxType())
                        .append(" | frequency ").append(obligation.frequency())
                        .append(" | rule version ").append(obligation.ruleVersion() == null ? "unconfigured" : obligation.ruleVersion())
                        .append(" | reason: ").append(obligation.reason())
                        .append('\n');
            }
        }
        context.append("Note: monetary amounts are produced only by the deterministic engine and filing periods; "
                + "they are not included here and must not be estimated.");
        return context.toString();
    }

    private String truncate(String value) {
        return value.length() <= 200 ? value : value.substring(0, 200);
    }
}
