package com.taxpilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.taxpilot.domain.business.Business;
import com.taxpilot.domain.business.BusinessMembership;
import com.taxpilot.domain.business.BusinessType;
import com.taxpilot.dto.DocumentResponse;
import com.taxpilot.repository.BusinessMembershipRepository;
import com.taxpilot.repository.DocumentRepository;
import com.taxpilot.service.AuditService;
import com.taxpilot.service.DocumentService;
import com.taxpilot.storage.DocumentStorageService;
import com.taxpilot.storage.StoredObject;

class DocumentServiceTest {

    private final DocumentRepository documentRepository = Mockito.mock(DocumentRepository.class);
    private final BusinessMembershipRepository membershipRepository = Mockito.mock(BusinessMembershipRepository.class);
    private final DocumentStorageService storageService = Mockito.mock(DocumentStorageService.class);
    private final AuditService auditService = Mockito.mock(AuditService.class);

    private final UUID userId = UUID.randomUUID();
    private final UUID businessId = UUID.randomUUID();

    private DocumentService service;

    @BeforeEach
    void setUp() {
        service = new DocumentService(documentRepository, membershipRepository, storageService, auditService);
        Mockito.when(storageService.isConfigured()).thenReturn(true);
        Mockito.when(documentRepository.save(any())).thenAnswer(invocation -> {
            com.taxpilot.domain.document.Document document = invocation.getArgument(0);
            document.setId(UUID.randomUUID());
            return document;
        });
        Mockito.when(membershipRepository.findByBusinessIdAndUserId(businessId, userId))
                .thenReturn(Optional.of(membership()));
    }

    @Test
    void uploadsAllowedFileAndRecordsAudit() {
        Mockito.when(storageService.store(any(), anyString(), eq("application/pdf"), anyString()))
                .thenReturn(new StoredObject("cloudinary", "https://cdn.example/doc.pdf", "pub-1"));
        MockMultipartFile file = new MockMultipartFile("file", "vat-return.pdf", "application/pdf", "data".getBytes());

        DocumentResponse response = service.upload(userId, businessId, file, "VAT");

        assertEquals("vat-return.pdf", response.fileName());
        assertEquals("https://cdn.example/doc.pdf", response.url());
        Mockito.verify(auditService).record(any(), eq("DOCUMENT"), any(), eq(businessId), anyString());
    }

    @Test
    void rejectsUnsupportedFileType() {
        MockMultipartFile file = new MockMultipartFile("file", "virus.exe", "application/octet-stream", "data".getBytes());

        assertThrows(ResponseStatusException.class, () -> service.upload(userId, businessId, file, null));
        Mockito.verify(documentRepository, Mockito.never()).save(any());
    }

    @Test
    void rejectsOversizeFile() {
        byte[] big = new byte[10 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("file", "big.pdf", "application/pdf", big);

        assertThrows(ResponseStatusException.class, () -> service.upload(userId, businessId, file, null));
    }

    @Test
    void rejectsWhenNotAMember() {
        Mockito.when(membershipRepository.findByBusinessIdAndUserId(businessId, userId)).thenReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile("file", "ok.pdf", "application/pdf", "data".getBytes());

        assertThrows(ResponseStatusException.class, () -> service.upload(userId, businessId, file, null));
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
