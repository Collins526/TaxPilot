package com.taxpilot.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.taxpilot.domain.user.User;
import com.taxpilot.dto.DocumentResponse;
import com.taxpilot.service.DocumentService;

@RestController
@RequestMapping("/api/businesses")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(path = "/{businessId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(@AuthenticationPrincipal User user,
                                                   @PathVariable UUID businessId,
                                                   @RequestParam("file") MultipartFile file,
                                                   @RequestParam(value = "category", required = false) String category) {
        return ResponseEntity.ok(documentService.upload(user.getId(), businessId, file, category));
    }

    @GetMapping("/{businessId}/documents")
    public ResponseEntity<List<DocumentResponse>> list(@AuthenticationPrincipal User user,
                                                       @PathVariable UUID businessId) {
        return ResponseEntity.ok(documentService.list(user.getId(), businessId));
    }
}
