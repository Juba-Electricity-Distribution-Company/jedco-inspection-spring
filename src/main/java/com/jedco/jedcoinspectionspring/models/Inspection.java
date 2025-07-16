package com.jedco.jedcoinspectionspring.models;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "inspections")
public class Inspection extends BaseEntity{
    @Column(name="problem_type", nullable=false, length=100)
    private String problemTypes;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="registered_by", nullable=false)
    private User user;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="inspection_status", nullable=false)
    private Status status;

    @Column(name="customer_name", nullable=false, length=100)
    private String customerName;

    @Column(name="phone_no", length=100)
    private String phoneNo;

    @Column(name="meter_no", nullable=false, length=100)
    private String meterNo;

    @Column(name="ciu_no", length=100)
    private String ciuNo;

    @Column(name="ct_ratio", length = 20)
    private String ctRatio;

    @Column(name="conn_type", nullable=false, length=100)
    private String connectionType;

    @Column(name="tarif_cat", nullable=false, length=100)
    private String tariffCategory;

    @Column(name="location", length=250)
    private String location;

    @Column(name="feeder", nullable=false, length=100)
    private String feeder;

    @Column(name="tx_no", nullable=false, length=100)
    private String txNo;

    @Column(name="latitude", nullable=false, precision=10)
    private Double latitude;

    @Column(name="longitude", nullable=false, precision=10)
    private Double longitude;

    @Column(name="location_accuracy", precision=10)
    private Double locationAccuracy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="registered_on", length=23)
    private Date registeredOn;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="completed_on", length=23)
    private Date completedOn;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="approved_on", length=23)
    private Date approvedOn;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="updated_on", length=23)
    private Date updatedOn;

    @Column(name="cust_satisfaction_review", length=250)
    private String customerSatisfactionReview;

    @Column(name="remark", length=250)
    private String remark;

    @Column(name="meter_type", length=250)
    private String meterType;

    @Column(name="customer_status", length=250)
    private String customerStatus;

    @Column(name="legal_case_no",length = 20)
    private String legalCaseNo;

    @OneToMany(fetch=FetchType.LAZY, mappedBy="inspection")
    private Set<CheckListResult> installationChecklists = new HashSet<CheckListResult>(0);

    @OneToMany(fetch=FetchType.LAZY, mappedBy="inspection")
    private Set<SalesAssignment> salesAssignments = new HashSet<SalesAssignment>(0);

    @OneToMany(fetch=FetchType.LAZY, mappedBy="inspection")
    private Set<CodeResult> codeResults = new HashSet<CodeResult>(0);

    @OneToMany(fetch=FetchType.LAZY, mappedBy="inspection")
    private Set<TaskHistory> taskHistories = new HashSet<TaskHistory>(0);

    @OneToMany(fetch=FetchType.LAZY, mappedBy = "inspection")
    private Set<InspectionFile> inspectionFiles = new HashSet<>(0);

    @OneToMany(fetch=FetchType.LAZY, mappedBy = "inspection")
    private Set<InspectionAssessment> assessments = new HashSet<>(0);

}
