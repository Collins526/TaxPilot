package com.taxpilot.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.taxpilot.domain.audit.AuditAction;
import com.taxpilot.domain.business.Business;
import com.taxpilot.domain.business.BusinessMembership;
import com.taxpilot.domain.document.Document;
import com.taxpilot.dto.DocumentResponse;
import com.taxpilot.repository.BusinessMembershipRepository;
import com.taxpilot.repository.DocumentRepository;
import com.taxpilot.storage.DocumentStorageService;
import com.taxpilot.storage.StoredObject;

@Service
public class DocumentService {

    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "image/webp",
            "text/csv",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel"
    );

    private final DocumentRepository documentRepository;
    private final BusinessMembershipRepository membershipRepository;
    private final DocumentStorageService documentStorageService;
    private final AuditService auditService;

    public DocumentService(DocumentRepository documentRepository,
                           BusinessMembershipRepository membershipRepository,
                           DocumentStorageService documentStorageService,
                           AuditService auditService) {
        this.documentRepository = documentRepository;
        this.membershipRepository = membershipRepository;
        this.documentStorageService = documentStorageService;
        this.auditService = auditService;
    }

    @Transactional
    public DocumentResponse upload(UUID userId, UUID businessId, MultipartFile file, String category) {
        Business business = requireActiveMembership(userId, businessId).getBusiness();
        validate(file);
        if (!documentStorageService.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Document storage is not configured");
        }

        String fileName = sanitize(file.getOriginalFilename());
        String publicId = UUID.randomUUID() + "-" + fileName;
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();

        StoredObject stored;
        try {
            stored = documentStorageService.store(file.getBytes(), publicId, contentType, "taxpilot/" + businessId);
        } catch (java.io.IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file could not be read");
        }

        Document document = new Document();
        document.setBusiness(business);
        document.setFileName(fileName);
        document.setContentType(contentType);
        document.setSizeBytes(file.getSize());
        document.setCategory(category);
        document.setStorageProvider(stored.provider());
        document.setStorageUrl(stored.url());
        document.setStoragePublicId(stored.publicId());
        document.setUploadedBy(userId);
        document.setUploadedAt(LocalDateTime.now(ZoneOffset.UTC));
        Document saved = documentRepository.save(document);

        auditService.record(AuditAction.DOCUMENT_UPLOADED, "DOCUMENT", saved.getId(), businessId,
                "Document uploaded: " + fileName);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> list(UUID userId, UUID businessId) {
        requireActiveMembership(userId, businessId);
        return documentRepository.findByBusinessIdOrderByUploadedAtDesc(businessId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A file is required");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File exceeds the 10 MB limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Unsupported file type. Allowed: PDF, PNG, JPEG, WEBP, CSV, XLSX.");
        }
    }

    private String sanitize(String originalName) {
        String name = originalName == null || originalName.isBlank() ? "document" : originalName;
        String cleaned = name.replaceAll("[^A-Za-z0-9._-]", "_");
        return cleaned.length() > 180 ? cleaned.substring(cleaned.length() - 180) : cleaned;
    }

    private DocumentResponse toResponse(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getFileName(),
                document.getContentType(),
                document.getSizeBytes(),
                document.getCategory(),
                document.getStorageProvider(),
                document.getStorageUrl(),
                document.getUploadedAt()
        );
    }

    private BusinessMembership requireActiveMembership(UUID userId, UUID businessId) {
        return membershipRepository.findByBusinessIdAndUserId(businessId, userId)
                .filter(BusinessMembership::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found"));
    }
}
