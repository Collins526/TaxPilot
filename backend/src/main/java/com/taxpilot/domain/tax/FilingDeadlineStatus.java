package com.taxpilot.domain.tax;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.taxpilot.domain.business.Business;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "filing_deadline_statuses", uniqueConstraints = @UniqueConstraint(
        name = "uk_filing_deadline_status_business_tax_date",
        columnNames = {"business_id", "tax_type", "due_date"}))
@Getter
@Setter
@NoArgsConstructor
public class FilingDeadlineStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(name = "tax_type", nullable = false, length = 64)
    private String taxType;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "filing_status", nullable = false, length = 24)
    private FilingStatus filingStatus;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
