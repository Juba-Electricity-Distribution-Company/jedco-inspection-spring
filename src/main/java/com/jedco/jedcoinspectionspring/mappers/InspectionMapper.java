package com.jedco.jedcoinspectionspring.mappers;

import com.jedco.jedcoinspectionspring.models.*;
import com.jedco.jedcoinspectionspring.rest.responses.CheckListResultResponse;
import com.jedco.jedcoinspectionspring.rest.responses.CodeResultResponse;
import com.jedco.jedcoinspectionspring.rest.responses.InspectionResponse;
import com.jedco.jedcoinspectionspring.rest.responses.InspectionSalesResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import java.util.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface InspectionMapper {
    @Mapping(target = "tarifCat", source = "tariffCategory")
    @Mapping(target = "statusId", source = "status.id")
    @Mapping(target = "status", source = "status.name")
    @Mapping(target = "registeredDate", source = "registeredOn")
    @Mapping(target = "registeredBy", source = "user", qualifiedByName = "mapRegisteredBy")
    @Mapping(target = "problemType", source = "problemTypes")
    @Mapping(target = "phoneNumber", source = "phoneNo")
    @Mapping(target = "metterNumber", source = "meterNo")
    @Mapping(target = "connType", source = "connectionType")
    @Mapping(target = "completedDate", source = "completedOn")
    @Mapping(target = "ciuNumber", source = "ciuNo")
    @Mapping(target = "approvedDate", source = "approvedOn")
    @Mapping(target = "codeResultList", source = "codeResults", qualifiedByName = "mapSortedCodeResults")
    @Mapping(target = "checkListResultList", source = "installationChecklists")
    @Mapping(target = "filePath", source = "inspectionFiles", qualifiedByName = "mapFilePath")
    @Mapping(target = "assesmentSubmitted", source = "assessments", qualifiedByName = "mapAssessments")
    InspectionResponse toInspectionResponse(Inspection inspection);

    @Named("mapSortedCodeResults")
    default List<CodeResultResponse> mapSortedCodeResults(Set<CodeResult> codeResults) {
        if (codeResults == null || codeResults.isEmpty()) {
            return new ArrayList<>();
        }

        return codeResults.stream()
                .sorted(
                        Comparator
                                // Primary: orderIndex (0 is allowed)
                                .comparing((CodeResult cr) ->
                                        cr.getInspectionCode().getOrderIndex() == null
                                                ? 0
                                                : cr.getInspectionCode().getOrderIndex()
                                )
                                // Secondary: inspectionCode.id
                                .thenComparing(cr -> cr.getInspectionCode().getId())
                )
                .map(this::toCodeResultResponse)
                .toList();
    }


    @Named("mapRegisteredBy")
    default String mapRegisteredBy (User user){
        return user.getFirstName()+" "+user.getLastName();
    }

    @Named("mapFilePath")
    default List<String> mapFilePath(Set<InspectionFile> inspectionFiles){
        if(inspectionFiles==null || inspectionFiles.isEmpty()){
            return new ArrayList<>();
        }
        return inspectionFiles.stream().map(InspectionFile::getFileName).toList();
    }

    @Named("mapAssessments")
    default boolean mapAssessments(Set<InspectionAssessment> assessments){
        if(assessments == null) {
            return false;
        }
        if(assessments.isEmpty()){
            return false;
        }
        return true;
    }


    @Mapping(target = "checkListId", source = "id")
    @Mapping(target = "checkList",source = "checkList.checkList")
    CheckListResultResponse toCheckListResultResponse(CheckListResult checkListResult);

    @Mapping(target = "inspectionCodeId", source = "inspectionCode.id")
    @Mapping(target = "description", source = "inspectionCode.description")
    @Mapping(target = "inspectionCode",source = "inspectionCode.code")
    CodeResultResponse toCodeResultResponse(CodeResult codeResult);

    @Mapping(target = "tarifCat", source = "tariffCategory")
    @Mapping(target = "statusId", source = "status.id")
    @Mapping(target = "status", source = "status.name")
    @Mapping(target = "sentToSalesOn", source = "salesAssignments",qualifiedByName = "mapSentToSalesOn")
    @Mapping(target = "registeredDate", source = "registeredOn")
    @Mapping(target = "registeredBy", source = "user", qualifiedByName = "mapRegisteredBy")
    @Mapping(target = "problemType", source = "problemTypes")
    @Mapping(target = "priorityLevel", source = "salesAssignments", qualifiedByName = "mapPriorityLevel")
    @Mapping(target = "phoneNumber", source = "phoneNo")
    @Mapping(target = "metterNumber", source = "meterNo")
    @Mapping(target = "filePath", source = "inspectionFiles", qualifiedByName = "mapFilePath")
    @Mapping(target = "connType", source = "connectionType")
    @Mapping(target = "codeResultList", source = "codeResults")
    @Mapping(target = "ciuNumber", source = "ciuNo")
    @Mapping(target = "checkListResultList", source = "installationChecklists")
    @Mapping(target = "assignmentNote", source = "salesAssignments", qualifiedByName = "mapAssignmentNote")
    @Mapping(target = "assesmentSubmitted", source = "assessments", qualifiedByName = "mapAssessments")
    @Mapping(target = "legalCaseNo", source = "legalCaseNo", qualifiedByName = "mapLegalCaseNo")
    InspectionSalesResponse toInspectionSalesResponse(Inspection inspection);

    @Named("mapLegalCaseNo")
    default String mapLegalCasNo(String legalCaseNo){
        if(legalCaseNo==null)
            return "";
        return legalCaseNo;
    }

    @Named("mapAssignmentNote")
    default String mapAssignmentNote(Set<SalesAssignment> assessments){
        return assessments.iterator().next().getNote();
    }
    @Named("mapPriorityLevel")
    default String mapPriorityLevel(Set<SalesAssignment> assessments){
        return assessments.iterator().next().getPriority().getName();
    }

    @Named("mapSentToSalesOn")
    default Date mapSentToSalesOn(Set<SalesAssignment> assignments){
        return assignments.iterator().next().getSalesSentDate();
    }



}
