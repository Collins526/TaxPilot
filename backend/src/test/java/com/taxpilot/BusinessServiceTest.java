package com.taxpilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.taxpilot.domain.business.BusinessType;
import com.taxpilot.domain.user.Role;
import com.taxpilot.domain.user.User;
import com.taxpilot.dto.BusinessResponse;
import com.taxpilot.dto.CreateBusinessRequest;
import com.taxpilot.dto.FilingStatusRequest;
import com.taxpilot.domain.tax.FilingStatus;
import com.taxpilot.dto.AuthenticationRequest;
import com.taxpilot.dto.AuthenticationResponse;
import com.taxpilot.dto.RegistrationRequest;
import com.taxpilot.repository.UserRepository;
import com.taxpilot.repository.TaxProfileRepository;
import com.taxpilot.service.BusinessService;
import com.taxpilot.service.AuthService;
import com.taxpilot.service.TaxProfileService;
import com.taxpilot.tax.TaxObligation;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@Transactional
class BusinessServiceTest {

    @Autowired
    private BusinessService businessService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaxProfileRepository taxProfileRepository;

    @Autowired
    private TaxProfileService taxProfileService;

    @Autowired
    private AuthService authService;

    @Test
    void createBusinessAddsMembership() {
        AuthenticationResponse registered = authService.register(new RegistrationRequest(
                "Sam",
                "Test",
                "sam.test@example.com",
                "+254711111111",
                "correct-horse-battery"
        ));
        assertNotNull(registered.accessToken());
        assertNotNull(authService.authenticate(new AuthenticationRequest(
                "sam.test@example.com",
                "correct-horse-battery"
        )).accessToken());

        User user = userRepository.save(User.builder()
                .firstName("Jane")
                .lastName("Njeri")
                .email("jane.njeri@example.com")
                .phone("+254712345678")
                .passwordHash("hashed-password")
                .roles(new java.util.HashSet<>(java.util.List.of(Role.BUSINESS_OWNER)))
                .enabled(true)
                .build());

        BusinessResponse response = businessService.createBusiness(user.getId(), new CreateBusinessRequest(
                "Njeri Traders",
                "Jane Njeri",
                "PVT-1001",
                "A123456789Z",
                "Retail",
                BusinessType.SOLE_PROPRIETOR,
                java.math.BigDecimal.valueOf(2500000),
                5,
                true,
                true,
                "KE",
                "Africa/Nairobi"
        ));

        assertNotNull(response.id());
        assertEquals("Njeri Traders", response.name());
        assertEquals(1, businessService.listBusinessesForUser(user.getId()).size());
        assertNotNull(taxProfileRepository.findByBusinessId(response.id()).orElse(null));

        List<TaxObligation> obligations = businessService.getObligations(user.getId(), response.id());
        assertEquals(List.of("PAYE", "VAT", "INCOME_TAX"),
                obligations.stream().map(TaxObligation::taxType).toList());
        assertEquals("KE-CALENDAR-2026.1", obligations.get(0).ruleVersion());
        assertNull(obligations.get(0).estimatedAmount());
        var calendar = businessService.getCalendar(user.getId(), response.id());
        assertNotNull(calendar);
        assertEquals(FilingStatus.NOT_STARTED, calendar.get(0).filingStatus());
        var updatedDeadline = businessService.updateFilingStatus(user.getId(), response.id(),
                new FilingStatusRequest(calendar.get(0).taxType(), calendar.get(0).dueDate(), FilingStatus.IN_PROGRESS));
        assertEquals(FilingStatus.IN_PROGRESS, updatedDeadline.filingStatus());
        assertEquals(FilingStatus.IN_PROGRESS,
                businessService.getCalendar(user.getId(), response.id()).get(0).filingStatus());
        assertEquals("Njeri Traders", businessService.getDashboardSummary(user.getId(), response.id()).businessName());

        User otherUser = userRepository.save(User.builder()
                .firstName("Alex")
                .lastName("Other")
                .email("alex.other@example.com")
                .phone("+254700000002")
                .passwordHash("hashed-password")
                .roles(new java.util.HashSet<>(java.util.List.of(Role.BUSINESS_OWNER)))
                .enabled(true)
                .build());
        assertThrows(ResponseStatusException.class,
                () -> taxProfileService.getTaxProfile(otherUser.getId(), response.id()));
        assertThrows(ResponseStatusException.class,
                () -> businessService.getDashboardSummary(otherUser.getId(), response.id()));
        assertThrows(ResponseStatusException.class,
                () -> businessService.updateFilingStatus(otherUser.getId(), response.id(),
                        new FilingStatusRequest(calendar.get(0).taxType(), calendar.get(0).dueDate(), FilingStatus.FILED)));
    }
}
