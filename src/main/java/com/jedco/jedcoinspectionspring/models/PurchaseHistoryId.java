package com.jedco.jedcoinspectionspring.models;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseHistoryId implements Serializable {
    private String meterNumber;
    private LocalDate created;
    private BigDecimal kwh;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PurchaseHistoryId that)) return false;
        return Objects.equals(meterNumber, that.meterNumber)
                && Objects.equals(created, that.created)
                && Objects.equals(kwh, that.kwh);
    }

    @Override
    public int hashCode() {
        return Objects.hash(meterNumber, created, kwh);
    }
}
