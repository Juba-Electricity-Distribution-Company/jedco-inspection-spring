package com.jedco.jedcoinspectionspring.controllers;

import com.jedco.jedcoinspectionspring.rest.requests.*;
import com.jedco.jedcoinspectionspring.rest.responses.*;
import com.jedco.jedcoinspectionspring.services.InspectionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/inspections")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Inspections")
public class InspectionController {
    private final InspectionService inspectionService;

    @GetMapping("/getPriorityLevels")
    public List<PriorityResponse> listAllPriorities() {
        return inspectionService.priorityList();
    }

    @GetMapping("/getInspectionCodes")
    public List<InspectionCodesResponse> getInspectionCodes(@RequestParam("meterType") String meterType) {
        return inspectionService.inspectionCodesList(meterType);
    }

    @GetMapping("/getInspectionCodesByProblemTypes")
    public List<InspectionCodesResponse> getInspectionCodesByProblemTypes(@RequestParam("problemTypeIds") List<Long> problemTypeIds) {
        return inspectionService.inspectionCodesByProblemTypes(problemTypeIds);
    }

    @PostMapping("/insertInspection")
    @PreAuthorize("hasAnyAuthority('REGISTER_INSPECTION')")
    public ResponseDTO register(@RequestBody InspectionInsertRequest insertDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return this.inspectionService.insertInspection(insertDto, userDetails.getUsername());
    }

    @PutMapping("/updateCustomerInfo/{inspectionId}")
    @PreAuthorize("hasAnyAuthority('UPDATE_CUSTOMER_INFO')")
    public ResponseDTO updateCustomerInfo(@PathVariable Long inspectionId,@RequestBody UpdateCustomerInfoRequest request){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return this.inspectionService.updateCustomerInfo(inspectionId,request,userDetails.getUsername());
    }

    @GetMapping("/getInspectionSByDate")
    @PreAuthorize("hasAnyAuthority('VIEW_INSPECTION', 'REGISTER_INSPECTION', 'UPDATE_INSPECTION')")
    public List<InspectionResponse> getDailyInspectionS(@RequestParam("startDate") String startDate, @RequestParam("endDate") String endDate) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return this.inspectionService.inspectionsListByDate(startDate, endDate, userDetails.getUsername());
    }

    @GetMapping("/getAdminInspectionSByDate")
    @PreAuthorize("hasAnyAuthority('VIEW_INSPECTION', 'REGISTER_INSPECTION', 'UPDATE_INSPECTION')")
    public AdminInspectionResponse getAdminInspectionSByDate(@RequestParam(value = "startDate", required = false) String startDate,
                                                             @RequestParam(value = "endDate", required = false) String endDate,
                                                             @RequestParam(value = "page",defaultValue = "1") Integer page,
                                                             @RequestParam(value = "limit", defaultValue = "20") Integer limit,
                                                             @RequestParam(value = "customerName", required = false) String customerName,
                                                             @RequestParam(value = "metterNumber", required = false) String meterNumber,
                                                             @RequestParam(value = "statuses", required = false) List<Long> statuses,
                                                             @RequestParam(value = "problemType", required = false) String problemType,
                                                             @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
                                                             @RequestParam(value = "sort", required = false) String sort
                                                             ) {
        return this.inspectionService.adminInspectionsListByDate(startDate, endDate,customerName,meterNumber,phoneNumber,statuses,problemType, page,limit,sort);
    }

    @GetMapping("/exportAdminToExcel")
    public ResponseEntity<byte[]> exportInspectionsToExcel(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "customerName", required = false) String customerName,
            @RequestParam(value = "meterNumber", required = false) String meterNumber,
            @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
            @RequestParam(value = "statuses", required = false) List<Long> statuses,
            @RequestParam(value = "problemType", required = false) String problemType,
            @RequestParam(value = "sort", required = false) String sort
    ) {
        byte[] excelData = inspectionService.exportInspectionsToExcel(startDate, endDate, customerName, meterNumber,phoneNumber, statuses,problemType, sort);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "inspections.xlsx");
        headers.setContentLength(excelData.length);
        return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
    }

    @GetMapping("/sendInspectionsToSales")
    @PreAuthorize("hasAnyAuthority('VIEW_INSPECTION', 'REGISTER_INSPECTION', 'UPDATE_INSPECTION')")
    public ResponseDTO sendInspectionsToSales(@RequestParam("inspectionId") Long inspectionId, @RequestParam("priorityId") Long priorityId, @RequestParam("note") String note) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return this.inspectionService.sendToSales(inspectionId, priorityId, note, userDetails.getUsername());
    }

    @GetMapping("/getInspectionsByDateAndStatus")
    @PreAuthorize("hasAnyAuthority('VIEW_INSPECTION', 'REGISTER_INSPECTION', 'UPDATE_INSPECTION')")
    public List<InspectionResponse> getInspectionByDate(@RequestParam("startDate") String startDate, @RequestParam("endDate") String endDate, @RequestParam("statusId") Long statusId) {
        return this.inspectionService.getInspectionsByDateAndStatus(startDate, endDate, statusId);
    }

    @PostMapping("/uploadFile")
    public ResponseDTO uploadFile(  @RequestPart("uploadedFile") MultipartFile file,
                                    @RequestParam("assignmentId") Long inspectionId,
                                    @RequestParam("fileName") String fileName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        FileUploadFormRequest form = new FileUploadFormRequest(file, inspectionId, fileName);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return this.inspectionService.upload(form, userDetails.getUsername());
    }

    @PutMapping("/updateCheckList")
    @PreAuthorize("hasAnyAuthority('REGISTER_INSPECTION')")
    public ResponseDTO updateCheckList (@RequestBody List<UpdateCheckList> request){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return this.inspectionService.updateCheckList(request,userDetails.getUsername());
    }

    @PutMapping("/updateCodeResult/{inspectionId}")
    @PreAuthorize("hasAnyAuthority('REGISTER_INSPECTION')")
    public ResponseDTO updateCodeResult(@PathVariable Long inspectionId, @RequestBody List<CodeResultUpdateRequest> codeResults ){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return this.inspectionService.updateCodeResult(inspectionId,codeResults,userDetails.getUsername());
    }

    @PutMapping("/updateRemark/{inspectionId}")
    @PreAuthorize("hasAnyAuthority('REGISTER_INSPECTION')")
    public ResponseDTO updateRemark(@PathVariable Long inspectionId, @RequestBody UpdateRemarkRequest remark ){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return this.inspectionService.updateRemark(inspectionId,remark,userDetails.getUsername());
    }

}
