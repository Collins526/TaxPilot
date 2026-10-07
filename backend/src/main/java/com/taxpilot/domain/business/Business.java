package com.taxpilot.domain.business;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "businesses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Business {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String legalName;

    @Column(nullable = false, unique = true)
    private String registrationNumber;

    @Column(nullable = false)
    private String kraPin;

    @Column(nullable = false)
    private String industry;

    @Column(precision = 19, scale = 2)
    private java.math.BigDecimal annualTurnover;

    @Column
    private Integer employeeCount;

    @Column(nullable = false)
    private boolean vatRegistered;

    @Column(nullable = false)
    private boolean etimsRegistered;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BusinessType businessType;

    @Column(nullable = false)
    private String countryCode = "KE";

    @Column(nullable = false)
    private String timezone = "Africa/Nairobi";

    @OneToMany(mappedBy = "business")
    private List<BusinessMembership> memberships = new ArrayList<>();
}
