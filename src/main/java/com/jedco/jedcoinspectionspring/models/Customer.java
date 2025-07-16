package com.jedco.jedcoinspectionspring.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "vw_meter_customer")
public class Customer {
    @Id
    @Column(name="meter_no", length=100)
    private String meterNo;

    @Column(name="CustomerName", length=100)
    private String name;

    @Column(name="meter_type", length=100)
    private String meterType;

    @Column(name="connection_type", length=100)
    private String connectionType;

    @Column(name="tariff", length=100)
    private String tariff;

    @Column(name="feeder", length=100)
    private String feeder;

    @Column(name="tx_no", length=100)
    private String txNo;

    @Column(name="ct_ratio", length = 100)
    private String ctRatio;

}
