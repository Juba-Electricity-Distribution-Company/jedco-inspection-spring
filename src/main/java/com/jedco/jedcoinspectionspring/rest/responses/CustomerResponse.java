package com.jedco.jedcoinspectionspring.rest.responses;

public record CustomerResponse(
      String meterNo,
        String name,
        String meterType,
        String connectionType,
        String tariff,
        String feeder,
        String tx_no,
        String ctRatio
) {
}
