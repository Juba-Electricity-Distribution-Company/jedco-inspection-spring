package com.jedco.jedcoinspectionspring.repositories;

import com.jedco.jedcoinspectionspring.models.PurchaseHistory;
import com.jedco.jedcoinspectionspring.models.PurchaseHistoryId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseHistoryRepository extends JpaRepository<PurchaseHistory, PurchaseHistoryId> {
    List<PurchaseHistory> findByMeterNumberOrderByCreatedDesc(String meterIdentifier);
}
