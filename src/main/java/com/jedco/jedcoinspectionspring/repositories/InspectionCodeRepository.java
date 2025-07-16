package com.jedco.jedcoinspectionspring.repositories;

import com.jedco.jedcoinspectionspring.models.InspectionCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InspectionCodeRepository extends JpaRepository<InspectionCode,Long> {
    List<InspectionCode> findAllByMeterType(String meterType);

    @Query("SELECT DISTINCT ic FROM InspectionCode ic JOIN ic.problemTypes pt WHERE pt.id IN :problemTypeIds")
    List<InspectionCode> findDistinctByProblemTypeIds(@Param("problemTypeIds") List<Long> problemTypeIds);

}
