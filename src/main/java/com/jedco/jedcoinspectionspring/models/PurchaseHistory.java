package com.jedco.jedcoinspectionspring.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@Entity
@Immutable
@AllArgsConstructor
@NoArgsConstructor
@IdClass(PurchaseHistoryId.class)
@Table(name = "CustLastThreeTxns")
public class PurchaseHistory {
    @Id
    @Column(name = "MeterIdentifier", length = 50)
    private String meterNumber;

    @Id
    @Column(name = "Created")
    private LocalDate created;

    @Id
    @Column(name = "Kwh", nullable = false, precision = 38, scale = 6)
    private BigDecimal kwh;
}
