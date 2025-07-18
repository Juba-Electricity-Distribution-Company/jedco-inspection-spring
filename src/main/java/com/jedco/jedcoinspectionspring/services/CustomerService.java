package com.jedco.jedcoinspectionspring.services;

import com.jedco.jedcoinspectionspring.rest.responses.CustomerResponseDto;
import com.jedco.jedcoinspectionspring.rest.responses.PurchaseHistoryResponse;

import java.util.List;

public interface CustomerService {
    CustomerResponseDto getCustomer(String meterNo);

    List<PurchaseHistoryResponse> getPurchaseHistory(String meterNumber);
}
