package com.jedco.jedcoinspectionspring.services;

import com.jedco.jedcoinspectionspring.rest.requests.*;
import com.jedco.jedcoinspectionspring.rest.responses.*;

import java.util.List;

public interface InspectionService {
    ResponseDTO insertInspection(InspectionInsertRequest insertDto, String username);

    List<PriorityResponse> priorityList();

    List<InspectionResponse> inspectionsListByDate(String startDate, String endDate, String username);

    AdminInspectionResponse adminInspectionsListByDate(String startDate, String endDate,String customerName,String meterNumber,String phoneNumber,List<Long> statuses,String problemType, int page, int limit,String sort);

    byte[] exportInspectionsToExcel(String startDateString, String endDateString, String customerName, String meterNumber, String phoneNumber, List<Long> statuses,String problemType, String sort);

    List<InspectionResponse> getInspectionsByDateAndStatus(String startDate, String endDate, Long statusId);

    List<InspectionCodesResponse> inspectionCodesList(String meterType);

    ResponseDTO sendToSales(Long inspectionId, Long priorityId, String note, String username);

    ResponseDTO upload(FileUploadFormRequest form, String username);

    ResponseDTO updateCustomerInfo(Long inspectionId, UpdateCustomerInfoRequest request, String username);

    ResponseDTO updateCheckList(List<UpdateCheckList> request, String username);

    ResponseDTO updateCodeResult(Long inspectionId, List<CodeResultUpdateRequest> codeResults, String username);

    ResponseDTO updateRemark(Long inspectionId, UpdateRemarkRequest remark, String username);

    List<InspectionCodesResponse> inspectionCodesByProblemTypes(List<Long> problemTypeIds);
}
