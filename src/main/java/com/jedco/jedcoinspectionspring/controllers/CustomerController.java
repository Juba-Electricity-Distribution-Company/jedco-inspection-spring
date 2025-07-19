package com.jedco.jedcoinspectionspring.controllers;

import com.jedco.jedcoinspectionspring.rest.responses.CustomerResponseDto;
import com.jedco.jedcoinspectionspring.rest.responses.PurchaseHistoryResponse;
import com.jedco.jedcoinspectionspring.services.CustomerService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/customerService")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "CustomerService")
public class CustomerController {
    private final CustomerService customerService;

    @GetMapping("/confirmCustomer")
    @PreAuthorize("hasAnyAuthority('REGISTER_INSPECTION')")
    public CustomerResponseDto confirmCustomer(@RequestParam("meterNumber") String meterNumber) {
        return this.customerService.getCustomer(meterNumber);
    }

    @GetMapping("purchaseHistory")
    @PreAuthorize("hasAnyAuthority('REGISTER_INSPECTION')")
    public List<PurchaseHistoryResponse> getPurchaseHistory(@RequestParam("meterNumber") String meterNumber) {
        return this.customerService.getPurchaseHistory(meterNumber);
    }

}
