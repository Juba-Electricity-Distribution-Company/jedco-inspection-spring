package com.jedco.jedcoinspectionspring.services;

import com.jedco.jedcoinspectionspring.mappers.AssessmentMapper;
import com.jedco.jedcoinspectionspring.mappers.EquipmentMapper;
import com.jedco.jedcoinspectionspring.models.*;
import com.jedco.jedcoinspectionspring.repositories.*;
import com.jedco.jedcoinspectionspring.rest.requests.CustomerLoadInsertRequest;
import com.jedco.jedcoinspectionspring.rest.requests.CustomerLoadUpdateRequest;
import com.jedco.jedcoinspectionspring.rest.requests.LoadAssessmentInsertRequest;
import com.jedco.jedcoinspectionspring.rest.requests.LoadAssessmentUpdateRequest;
import com.jedco.jedcoinspectionspring.rest.responses.EquipmentResponse;
import com.jedco.jedcoinspectionspring.rest.responses.LoadAssessmentResponse;
import com.jedco.jedcoinspectionspring.rest.responses.ResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssessmentServiceImpl implements AssessmentService {
    private final EquipmentRepository equipmentRepository;
    private final InspectionAssessmentRepository assessmentRepository;
    private final UserRepository userRepository;
    private final InspectionRepository inspectionRepository;
    private final StatusRepository statusRepository;
    private final CustomerLoadRepository customerLoadRepository;
    private final EquipmentMapper equipmentMapper;
    private final AssessmentMapper assessmentMapper;
    private final AsyncService asyncService;
    @Override
    public List<EquipmentResponse> listAll() {
        var equipments = equipmentRepository.findAll();
        return equipments.stream().map(equipmentMapper::toEquipmentResponse).toList();
    }

    @Override
    public ResponseDTO insertAssessment(LoadAssessmentInsertRequest insertDto, String username) {
        Optional<Inspection> optionalInspection = inspectionRepository.findById(insertDto.inspectionId());
        Optional<User> optionalRegisteredBy = userRepository.findByUsername(username);
        if(optionalInspection.isEmpty()){
            return new ResponseDTO(false, "Inspection Not Found.");
        }
        if(optionalRegisteredBy.isEmpty()){
            return new ResponseDTO(false, "User Not Found.");
        }
        Inspection inspection= optionalInspection.get();
        User registeredBy= optionalRegisteredBy.get();
        Date regDate = new Date();
        Status activeStatus = statusRepository.findById(1L).get();

        InspectionAssessment assessment = new InspectionAssessment();
        assessment.setInspection(inspection);
        assessment.setCustomerType(insertDto.customerType());
        assessment.setPresentedDocument(insertDto.presentedDocument());
        assessment.setReason(insertDto.reason());
        assessment.setRegisteredOn(regDate);
        assessment.setUser(registeredBy);
        assessment.setStatus(activeStatus);

        assessmentRepository.save(assessment);

        for (CustomerLoadInsertRequest loadInsertDto : insertDto.customerLoadInsertDto()) {
            CustomerLoad customerLoad = new CustomerLoad();
            Equipment equipments = equipmentRepository.findById(loadInsertDto.equipmentId()).get();

            customerLoad.setEquipment(equipments);
            customerLoad.setInspectionAssesment(assessment);
            customerLoad.setPowerRate(loadInsertDto.powerRate());
            customerLoad.setQuantity(loadInsertDto.quantity());
            customerLoad.setTotalKwh(loadInsertDto.totalKwh());
            customerLoad.setRegisteredOn(regDate);
            customerLoad.setUser(registeredBy);
            customerLoad.setStatus(activeStatus);

            customerLoadRepository.save(customerLoad);

        }


        TaskHistory taskHistory = new TaskHistory();

        taskHistory.setActionDate(regDate);
        taskHistory.setInspection(inspection);
        taskHistory.setActionBy(registeredBy);
        taskHistory.setActionType("INSPECTION ASSESSMENT SUBMITTED");

        taskHistory.setHistoryDetails(registeredBy.getFirstName() + " " + registeredBy.getLastName() + " Submitted Inspection Assessment");
        this.asyncService.postHistory(taskHistory);
        return new ResponseDTO(true, "Assessment Registered Successfully.");


    }

    @Override
    public List<LoadAssessmentResponse> listAssessmentsByInspection(Long inspectionId) {
        var assessment= assessmentRepository.findFirstByInspection_Id(inspectionId);
        return Collections.singletonList(assessmentMapper.toLoadAssessmentResponse(assessment));

    }

    @Override
    public ResponseDTO updateAssessment(Long inspectionId, LoadAssessmentUpdateRequest request, String username) {

        if (request.id() == null) {
            List<CustomerLoadInsertRequest> loadInsertRequests = request
                    .customerLoadUpdateRequestList()
                    .stream()
                    .map(assessmentMapper::toCustomerLoadInsertRequest)
                    .toList();

            LoadAssessmentInsertRequest insertRequest = new LoadAssessmentInsertRequest(
                    request.customerType(),
                    request.presentedDocument(),
                    "Inspection",
                    inspectionId,
                    loadInsertRequests
            );

            return insertAssessment(insertRequest, username);
        }

        Optional<Inspection> optionalInspection = inspectionRepository.findById(inspectionId);
        if (optionalInspection.isEmpty()) {
            return new ResponseDTO(false, "Inspection not found!");
        }

        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isEmpty()) {
            return new ResponseDTO(false, "User not found!");
        }

        Optional<InspectionAssessment> optionalInspectionAssessment = assessmentRepository.findById(request.id());
        if (optionalInspectionAssessment.isEmpty()) {
            return new ResponseDTO(false, "Assessment not found");
        }

        var assessment = optionalInspectionAssessment.get();
        User user = optionalUser.get();
        Inspection inspection = optionalInspection.get();

        StringBuilder additionalNoteBuilder = new StringBuilder();
        additionalNoteBuilder.append("Updated fields:").append(System.lineSeparator());

        if (!Objects.equals(assessment.getCustomerType(), request.customerType())) {
            additionalNoteBuilder.append("Customer Type: ")
                    .append(assessment.getCustomerType())
                    .append(" -> ")
                    .append(request.customerType())
                    .append(System.lineSeparator());

            assessment.setCustomerType(request.customerType());
        }

        if (!Objects.equals(assessment.getPresentedDocument(), request.presentedDocument())) {
            additionalNoteBuilder.append("Presented Document: ")
                    .append(assessment.getPresentedDocument())
                    .append(" -> ")
                    .append(request.presentedDocument())
                    .append(System.lineSeparator());

            assessment.setPresentedDocument(request.presentedDocument());
        }

        if (!Objects.equals(assessment.getReason(), request.reason())) {
            additionalNoteBuilder.append("Reason: ")
                    .append(assessment.getReason())
                    .append(" -> ")
                    .append(request.reason())
                    .append(System.lineSeparator());

            assessment.setReason(request.reason());
        }

        List<Long> requestCustLoadIdList = request.customerLoadUpdateRequestList()
                .stream()
                .map(CustomerLoadUpdateRequest::id)
                .toList();

        Iterator<CustomerLoad> iterator = assessment.getCustomerLoads().iterator();

        while (iterator.hasNext()) {
            CustomerLoad custLoad = iterator.next();

            if (!requestCustLoadIdList.contains(custLoad.getId())) {
                additionalNoteBuilder.append("Deleted Customer Load: ")
                        .append(custLoad.getEquipment().getName())
                        .append(System.lineSeparator());

                iterator.remove();
                customerLoadRepository.delete(custLoad);
            }
        }

        for (var custLoadRequest : request.customerLoadUpdateRequestList()) {

            CustomerLoad customerLoad;

            if (custLoadRequest.id() != null) {

                Optional<CustomerLoad> optionalCustomerLoad = customerLoadRepository.findById(custLoadRequest.id());

                if (optionalCustomerLoad.isEmpty()) {
                    return new ResponseDTO(false, "Customer Load not found!");
                }

                customerLoad = optionalCustomerLoad.get();

                if (!Objects.equals(customerLoad.getPowerRate(), custLoadRequest.powerRate())) {

                    additionalNoteBuilder.append("Power Rate of Equipment ")
                            .append(customerLoad.getEquipment().getName())
                            .append(" :")
                            .append(customerLoad.getPowerRate())
                            .append(" -> ")
                            .append(custLoadRequest.powerRate())
                            .append(System.lineSeparator());

                    customerLoad.setPowerRate(custLoadRequest.powerRate());
                }

                if (!Objects.equals(customerLoad.getQuantity(), custLoadRequest.quantity())) {

                    additionalNoteBuilder.append("Quantity of Equipment ")
                            .append(customerLoad.getEquipment().getName())
                            .append(" :")
                            .append(customerLoad.getQuantity())
                            .append(" -> ")
                            .append(custLoadRequest.quantity())
                            .append(System.lineSeparator());

                    customerLoad.setQuantity(custLoadRequest.quantity());
                }

                if (!Objects.equals(customerLoad.getTotalKwh(), custLoadRequest.totalKwh())) {

                    additionalNoteBuilder.append("Total Kwh of Equipment ")
                            .append(customerLoad.getEquipment().getName())
                            .append(" :")
                            .append(customerLoad.getTotalKwh())
                            .append(" -> ")
                            .append(custLoadRequest.totalKwh())
                            .append(System.lineSeparator());

                    customerLoad.setTotalKwh(custLoadRequest.totalKwh());
                }

                customerLoad.setUpdatedOn(new Date());

            } else {

                if (custLoadRequest.equipmentId() == null) {
                    return new ResponseDTO(false, "Equipment is empty");
                }

                Optional<Equipment> optionalEquipment = equipmentRepository.findById(custLoadRequest.equipmentId());

                if (optionalEquipment.isEmpty()) {
                    return new ResponseDTO(false, "Equipment not found!");
                }

                customerLoad = new CustomerLoad();
                customerLoad.setInspectionAssesment(assessment);
                customerLoad.setEquipment(optionalEquipment.get());
                customerLoad.setPowerRate(custLoadRequest.powerRate());
                customerLoad.setQuantity(custLoadRequest.quantity());
                customerLoad.setTotalKwh(custLoadRequest.totalKwh());
                customerLoad.setRegisteredOn(new Date());
                customerLoad.setUser(user);
                customerLoad.setStatus(statusRepository.findById(1L).get());

                additionalNoteBuilder.append("Add Customer Load :")
                        .append(optionalEquipment.get().getName())
                        .append(System.lineSeparator());
            }

            customerLoadRepository.save(customerLoad);
        }

        assessmentRepository.save(assessment);

        TaskHistory taskHistory = new TaskHistory();
        taskHistory.setInspection(inspection);
        taskHistory.setActionBy(user);
        taskHistory.setActionDate(new Date());
        taskHistory.setActionType("Assessment updated");
        taskHistory.setHistoryDetails(user.getFirstName() + " " + user.getLastName() + " Updated Code List");
        taskHistory.setAdditionalNote(additionalNoteBuilder.toString());

        this.asyncService.postHistory(taskHistory);

        return new ResponseDTO(true, "Assessment updated successfully!");
    }
}
