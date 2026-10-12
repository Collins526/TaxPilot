package com.taxpilot.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.taxpilot.domain.document.Document;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByBusinessIdOrderByUploadedAtDesc(UUID businessId);

    Optional<Document> findByIdAndBusinessId(UUID id, UUID businessId);
}
