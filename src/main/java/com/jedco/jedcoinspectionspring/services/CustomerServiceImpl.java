package com.jedco.jedcoinspectionspring.services;

import com.jedco.jedcoinspectionspring.mappers.CustomerMapper;
import com.jedco.jedcoinspectionspring.models.Customer;
import com.jedco.jedcoinspectionspring.models.PurchaseHistory;
import com.jedco.jedcoinspectionspring.repositories.CustomerRepository;
import com.jedco.jedcoinspectionspring.repositories.PurchaseHistoryRepository;
import com.jedco.jedcoinspectionspring.rest.responses.CustomerResponseDto;
import com.jedco.jedcoinspectionspring.rest.responses.PurchaseHistoryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {
    private final CustomerRepository customerRepository;
    private final PurchaseHistoryRepository purchaseHistoryRepository;
    private final CustomerMapper customerMapper;
    @Override
    public CustomerResponseDto getCustomer(String meterNo) {
        Optional<Customer> optionalCustomer= customerRepository.findFirstByMeterNo(meterNo);
        if(optionalCustomer.isEmpty()){
            return new CustomerResponseDto(false,"Customer Not Found in the system",null);
        }
        Customer customer=optionalCustomer.get();
        var customerResponse= customerMapper.toCustomerResponse(customer);
        return new CustomerResponseDto(true, "Customer Found in the system",customerResponse);
    }

    @Override
    public List<PurchaseHistoryResponse> getPurchaseHistory(String meterNumber) {
        List<PurchaseHistory> historyList =
                purchaseHistoryRepository.findByMeterNumberOrderByCreatedDesc(meterNumber);

        return historyList.stream()
                .map(txn -> new PurchaseHistoryResponse(
                        txn.getMeterNumber(),
                        txn.getCreated(),
                        txn.getKwh()
                ))
                .toList();
    }
}
