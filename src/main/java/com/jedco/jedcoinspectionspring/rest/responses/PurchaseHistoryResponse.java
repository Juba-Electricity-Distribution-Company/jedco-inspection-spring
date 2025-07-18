package com.jedco.jedcoinspectionspring.rest.responses;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PurchaseHistoryResponse(
        String meterNumber,
        LocalDate created,
        BigDecimal kwh
) {
}
