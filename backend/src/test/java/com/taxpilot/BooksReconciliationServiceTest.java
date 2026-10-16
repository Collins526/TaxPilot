package com.taxpilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.taxpilot.domain.audit.AuditAction;
import com.taxpilot.domain.business.Business;
import com.taxpilot.domain.business.BusinessMembership;
import com.taxpilot.domain.business.BusinessType;
import com.taxpilot.domain.tax.FilingPeriod;
import com.taxpilot.repository.BusinessMembershipRepository;
import com.taxpilot.repository.FilingPeriodRepository;
import com.taxpilot.service.AuditService;
import com.taxpilot.service.BooksReconciliationService;

class BooksReconciliationServiceTest {

    private final FilingPeriodRepository filingPeriodRepository = Mockito.mock(FilingPeriodRepository.class);
    private final BusinessMembershipRepository membershipRepository = Mockito.mock(BusinessMembershipRepository.class);
    private final AuditService auditService = Mockito.mock(AuditService.class);
    private final UUID userId = UUID.randomUUID();
    private final UUID businessId = UUID.randomUUID();
    private final UUID periodId = UUID.randomUUID();

    private BooksReconciliationService service;

    @BeforeEach
    void setUp() {
        service = new BooksReconciliationService(filingPeriodRepository, membershipRepository, auditService);
        Mockito.when(membershipRepository.findByBusinessIdAndUserId(businessId, userId))
                .thenReturn(Optional.of(membership()));
        Mockito.when(filingPeriodRepository.findByIdAndBusinessId(periodId, businessId))
                .thenReturn(Optional.of(period()));
    }

    @Test
    void reconcilesSalesAndPurchaseVatFromCsv() {
        MockMultipartFile file = csv("transaction_date,transaction_type,amount,vat_amount,description\r\n"
                + "2026-01-05,SALE,1000.00,160.00,\"market, stall 1\"\r\n"
                + "2026-01-06,PURCHASE,500.00,80.00,supplies\r\n");

        var response = service.reconcile(userId, businessId, periodId, file);

        assertEquals(2, response.transactionCount());
        assertEquals(new BigDecimal("1000.00"), response.bookSales());
        assertEquals(new BigDecimal("80.00"), response.purchaseInputVat());
        assertTrue(response.passed());
        assertEquals("MATCH", response.checks().get(0).status());
        assertEquals("MATCH", response.checks().get(1).status());
        assertEquals("MATCH", response.checks().get(2).status());
        Mockito.verify(auditService).record(eq(userId), eq(null), eq(AuditAction.BOOKS_RECONCILIATION_RUN),
                eq("FILING_PERIOD"), eq(periodId), eq(businessId), any(), anyString());
    }

    @Test
    void reportsMismatchesAndUnavailableRecordedValues() {
        FilingPeriod filingPeriod = period();
        filingPeriod.setTaxableSales(new BigDecimal("950"));
        filingPeriod.setEtimsDeclaredSales(null);
        filingPeriod.setInputVat(null);
        Mockito.when(filingPeriodRepository.findByIdAndBusinessId(periodId, businessId))
                .thenReturn(Optional.of(filingPeriod));

        var response = service.reconcile(userId, businessId, periodId,
                csv("transaction_date,transaction_type,amount,vat_amount\n"
                        + "2026-01-05,SALE,1000,0\n"));

        assertFalse(response.passed());
        assertEquals("MISMATCH", response.checks().get(0).status());
        assertEquals("NOT_AVAILABLE", response.checks().get(1).status());
        assertEquals("NOT_AVAILABLE", response.checks().get(2).status());
    }

    @Test
    void rejectsRowsOutsideFilingPeriod() {
        assertThrows(ResponseStatusException.class, () -> service.reconcile(userId, businessId, periodId,
                csv("transaction_date,transaction_type,amount,vat_amount\n"
                        + "2025-12-31,SALE,100,0\n")));
    }

    @Test
    void rejectsMissingHeadersAndInvalidAmounts() {
        assertThrows(ResponseStatusException.class, () -> service.reconcile(userId, businessId, periodId,
                csv("transaction_date,transaction_type,amount\n2026-01-05,SALE,100\n")));
        assertThrows(ResponseStatusException.class, () -> service.reconcile(userId, businessId, periodId,
                csv("transaction_date,transaction_type,amount,vat_amount\n"
                        + "2026-01-05,SALE,1000,0.123\n")));
    }

    @Test
    void rejectsNonMembers() {
        Mockito.when(membershipRepository.findByBusinessIdAndUserId(businessId, userId))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.reconcile(userId, businessId, periodId,
                csv("transaction_date,transaction_type,amount,vat_amount\n"
                        + "2026-01-05,SALE,100,0\n")));
    }

    private MockMultipartFile csv(String content) {
        return new MockMultipartFile("file", "books.csv", "text/csv", content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private FilingPeriod period() {
        FilingPeriod period = new FilingPeriod();
        period.setId(periodId);
        period.setBusiness(membership().getBusiness());
        period.setPeriodStart(LocalDate.of(2026, 1, 1));
        period.setPeriodEnd(LocalDate.of(2026, 1, 31));
        period.setTaxableSales(new BigDecimal("1000"));
        period.setEtimsDeclaredSales(new BigDecimal("1000"));
        period.setInputVat(new BigDecimal("80"));
        return period;
    }

    private BusinessMembership membership() {
        Business business = Business.builder()
                .id(businessId)
                .name("Njeri Traders")
                .legalName("Jane Njeri")
                .registrationNumber("PVT-1001")
                .kraPin("A123456789Z")
                .industry("Retail")
                .businessType(BusinessType.SOLE_PROPRIETOR)
                .countryCode("KE")
                .timezone("Africa/Nairobi")
                .build();
        return BusinessMembership.builder().business(business).active(true).build();
    }
}
