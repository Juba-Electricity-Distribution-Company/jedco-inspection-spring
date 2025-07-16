package com.jedco.jedcoinspectionspring.models;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "problem_types")
public class ProblemType extends BaseEntity{
    @Column(name="problem_type", nullable=false, length=100)
    private String problemType;
    @Column(name="type_description", length=250)
    private String typeDescription;

    @JsonManagedReference
    @ManyToMany
    @JoinTable(
            name = "inspection_code_problem_type",
            joinColumns = @JoinColumn(name = "problem_type_id"),
            inverseJoinColumns = @JoinColumn(name = "inspection_code_id")
    )
    private Set<InspectionCode> inspectionCodes;
}
