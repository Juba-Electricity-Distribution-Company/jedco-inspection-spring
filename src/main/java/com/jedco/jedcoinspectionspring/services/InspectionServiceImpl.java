package com.jedco.jedcoinspectionspring.services;

import com.jedco.jedcoinspectionspring.Util.DateConverter;
import com.jedco.jedcoinspectionspring.Util.Day;
import com.jedco.jedcoinspectionspring.mappers.InspectionCodeMapper;
import com.jedco.jedcoinspectionspring.mappers.InspectionMapper;
import com.jedco.jedcoinspectionspring.mappers.PriorityMapper;
import com.jedco.jedcoinspectionspring.models.*;
import com.jedco.jedcoinspectionspring.repositories.*;
import com.jedco.jedcoinspectionspring.rest.requests.*;
import com.jedco.jedcoinspectionspring.rest.responses.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class InspectionServiceImpl implements InspectionService {
    private final PriorityRepository priorityRepository;
    private final InspectionCodeRepository inspectionCodeRepository;
    private final UserRepository userRepository;
    private final StatusRepository statusRepository;
    private final InspectionRepository inspectionRepository;
    private final CheckListResultRepository checkListResultRepository;
    private final CheckListRepository checkListRepository;
    private final CodeResultRepository codeResultRepository;
    private final SalesAssignmentRepository salesAssignmentRepository;
    private final InspectionFileRepository inspectionFileRepository;
    private final AsyncService asyncService;
    private final ExcelGenerator excelGenerator;

    private final PriorityMapper priorityMapper;
    private final InspectionCodeMapper inspectionCodeMapper;
    private final InspectionMapper inspectionMapper;
    private final DateConverter dateConverter;
    private final PagingService pagingService;
    private final ProblemTypeRepository problemTypeRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;
    @Override
    public ResponseDTO insertInspection(InspectionInsertRequest insertDto, String username) {
        try {

            TaskHistory taskHistory = new TaskHistory();

            Inspection inspection = new Inspection();

            Date regDate = new Date();

            User registeredBy = userRepository.findByUsername(username).get();
            Status registeredStatus = statusRepository.findById(10L).get();

            if(insertDto.metterNumber().isEmpty()){
                //TODO Check if this statement is necessary
//                insertDto.setMetterNumber("");
                if(insertDto.remark()==null || insertDto.remark().isEmpty()){
                    taskHistory.setAdditionalNote("Inspection Registered without Meter Number");
                }
            }
            else{
                taskHistory.setAdditionalNote(insertDto.remark());
            }

            Date registeredOn = new Date();

            inspection.setProblemTypes(insertDto.problemType());
            inspection.setMeterType(insertDto.meterType());
            inspection.setCustomerName(insertDto.customerName());
            inspection.setPhoneNo(insertDto.phoneNumber());
            inspection.setMeterNo(insertDto.metterNumber());
            inspection.setCiuNo(insertDto.ciuNumber());
            inspection.setConnectionType(insertDto.connType());
            inspection.setTariffCategory(insertDto.tarifCat());
            inspection.setCtRatio(insertDto.ctRatio());
            inspection.setLocation(insertDto.location());
            inspection.setFeeder(insertDto.feeder());
            inspection.setTxNo(insertDto.txNo());
            inspection.setLatitude(insertDto.latitude());
            inspection.setLongitude(insertDto.longitude());
            inspection.setLocationAccuracy(insertDto.locationAccuracy());
            inspection.setRegisteredOn(registeredOn);
            inspection.setUser(registeredBy);
            inspection.setCustomerSatisfactionReview(insertDto.customerSatisfactionReview());
            inspection.setRemark(insertDto.remark());
            inspection.setStatus(registeredStatus);
            inspection.setCustomerStatus(insertDto.customerStatus());

            inspectionRepository.save(inspection);

            for (CheckListResultInsertRequest checkListResult : insertDto.checkListResultList()) {
                CheckListResult result = new CheckListResult();
                CheckList checkList = checkListRepository.findById(checkListResult.checkListId()).get();

                result.setCheckList(checkList);
                result.setChecklistStatus(checkListResult.checklistStatus());
                result.setInspection(inspection);
                result.setRegisteredOn(registeredOn);
                result.setRemark(checkListResult.remark());
                result.setUser(registeredBy);

                checkListResultRepository.save(result);

            }

            for (CodeResultInsertRequest resultInsertDto : insertDto.codeResultList()) {
                CodeResult codeResult = new CodeResult();
                InspectionCode inspectionCode = inspectionCodeRepository.findById(resultInsertDto.inspectionCodeId()).get();

                codeResult.setInspectionCode(inspectionCode);
                codeResult.setInspection(inspection);
                codeResult.setRegisteredOn(registeredOn);
                codeResult.setUser(registeredBy);
                codeResult.setResult(resultInsertDto.result());

                codeResultRepository.save(codeResult);

            }


            taskHistory.setActionDate(regDate);
            taskHistory.setInspection(inspection);
            taskHistory.setActionBy(registeredBy);
            taskHistory.setActionType("INSPECTION REGISTERED");

            taskHistory.setHistoryDetails(registeredBy.getFirstName() + " " + registeredBy.getLastName() + " Registered the Inspection");


            this.asyncService.postHistory(taskHistory);


            return new ResponseDTO(true, "Inspection Registered Successfully!");
        } catch (Exception ex) {
            log.error("ERROR registering Inspection -> " + ex.getMessage());
            return new ResponseDTO(false, "Inspection NOT Registered");

        }
    }

    @Override
    public List<PriorityResponse> priorityList() {
        var priorities= priorityRepository.findAll();
        return priorities.stream().map(priorityMapper::toPriorityResponse).toList();
    }

    @Override
    public List<InspectionResponse> inspectionsListByDate(String startDate, String endDate, String username) {
        Day day = dateConverter.convertBetweenDays(startDate, endDate);
        if (day == null) return null;
        Long deletedStatus=3L;
        User registeredBy = userRepository.findByUsername(username).get();
//        List<Inspection> inspections= inspectionRepository.findAllByStatusId_NotAndRegisteredOnBetween(deletedStatus,day.startTime(),day.endTime());
        List<Inspection> inspections= inspectionRepository.findAllByUserAndStatusId_NotAndRegisteredOnBetween(registeredBy,deletedStatus,day.startTime(),day.endTime());
        return inspections.stream().map(inspectionMapper::toInspectionResponse).toList();

    }

    @Override
    public AdminInspectionResponse adminInspectionsListByDate(String startDateString, String endDateString,String customerName,String meterNumber,List<Long> statuses,String problemType, int page, int limit,String sort) {
        Pageable pageable = pagingService.createPageable(page, limit, sort);
        Page<Inspection> inspectionPage=getAllInspectionsData(startDateString,endDateString,customerName,meterNumber,statuses,problemType,pageable);
        List<InspectionResponse> inspectionResponses = inspectionPage.getContent().stream()
                .map(inspectionMapper::toInspectionResponse)
                .toList();
        Long totalRows = inspectionPage.getTotalElements();
        return new AdminInspectionResponse(inspectionResponses, totalRows);

    }

    private Page<Inspection> getAllInspectionsData(String startDateString, String endDateString, String customerName, String meterNumber, List<Long> statuses, String problemType, Pageable pageable) {
        Day day = dateConverter.convertBetweenDays(startDateString, endDateString);
        Long deletedStatus = 3L;
        Page<Inspection> inspectionPage;

        Specification<Inspection> spec = Specification.where((root, query, criteriaBuilder) -> criteriaBuilder.notEqual(root.get("status").get("id"), deletedStatus));

        if (day != null) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.between(root.get("registeredOn"), day.startTime(), day.endTime()));
        }

        if (statuses != null && !statuses.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) -> root.get("status").get("id").in(statuses));
        }

        if (customerName != null && !customerName.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.like(criteriaBuilder.lower(root.get("customerName")), "%" + customerName.toLowerCase() + "%"));
        }

        if (meterNumber != null && !meterNumber.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.like(criteriaBuilder.lower(root.get("meterNo")), "%" + meterNumber.toLowerCase() + "%"));
        }

        if (problemType != null && !problemType.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.like(criteriaBuilder.lower(root.get("problemTypes")),"%"+problemType.toLowerCase()+"%"));
        }

        inspectionPage = inspectionRepository.findAll(spec, pageable);

        return inspectionPage;
    }
    // Method to export data to Excel
    public byte[] exportInspectionsToExcel(String startDateString, String endDateString, String customerName, String meterNumber, List<Long> statuses,String problemType, String sort) {
        Pageable pageable = pagingService.createPageable(sort);
        Page<Inspection> inspectionPage=getAllInspectionsData(startDateString,endDateString,customerName,meterNumber,statuses,problemType,pageable);
        List<InspectionResponse> inspections = inspectionPage.getContent().stream()
                .map(inspectionMapper::toInspectionResponse)
                .toList();
        return excelGenerator.generateExcel(inspections);
    }


    @Override
    public List<InspectionResponse> getInspectionsByDateAndStatus(String startDate, String endDate, Long statusId) {
        Day day = dateConverter.convertBetweenDays(startDate, endDate);
        if (day == null) return null;
        List<Inspection> inspections= inspectionRepository.findAllByStatusIdAndRegisteredOnBetween(statusId,day.startTime(),day.endTime());
        return inspections.stream().map(inspectionMapper::toInspectionResponse).toList();
    }

    @Override
    public List<InspectionCodesResponse> inspectionCodesList(String meterType) {
//        var inspectionCodes= inspectionCodeRepository.findAllByMeterType(meterType);
        var inspectionCodes = inspectionCodeRepository.findAllByMeterTypeOrderByOrderIndexAscIdAsc(meterType);
        return inspectionCodes.stream().map(inspectionCodeMapper::toResponse).toList();
    }

    @Override
    public ResponseDTO sendToSales(Long inspectionId, Long priorityId, String note, String username) {
        try {

            Optional<Inspection> optionalInspection = inspectionRepository.findById(inspectionId);
            Status sentToSalesStatus = statusRepository.findById(20L).get();
            Optional<User> optionalUser = userRepository.findByUsername(username);
            Optional<Priority> optionalPriority = priorityRepository.findById(priorityId);

            Date date = new Date();
            if(optionalInspection.isEmpty()){
                return new ResponseDTO(false, "Inspection Not Found!");
            }
            if(optionalUser.isEmpty()){
                return new ResponseDTO(false, "User Not Found!");
            }if(optionalPriority.isEmpty()){
                return new ResponseDTO(false, "Priority Not Found!");
            }
            Inspection inspection= optionalInspection.get();
            var priority= optionalPriority.get();
            var user= optionalUser.get();
            SalesAssignment assignment = new SalesAssignment();
            assignment.setInspection(inspection);
            assignment.setPriority(priority);
            assignment.setStatus(sentToSalesStatus);
            assignment.setSalesSentDate(date);
            assignment.setUser(user);
            assignment.setNote(note);

            salesAssignmentRepository.save(assignment);

            inspection.setStatus(sentToSalesStatus);
            inspection.setUpdatedOn(date);

            TaskHistory taskHistory = new TaskHistory();
            taskHistory.setAdditionalNote(note);
            taskHistory.setActionDate(date);
            taskHistory.setInspection(inspection);
            taskHistory.setActionBy(user);
            taskHistory.setActionType("INSPECTION SENT TO SALES");
            taskHistory.setHistoryDetails(user.getFirstName() + " " + user.getLastName() + " Sent the Inspection to Sales");
            this.asyncService.postHistory(taskHistory);

            return new ResponseDTO(true, "Inspection Sent to Sales Successfully");
        }catch (Exception ex){
            log.error("Send Inspection to Sales Failed. "+ex.getMessage());
            return new ResponseDTO(false, "Send Inspection to Sales Failed.");

        }
    }

    @Override
    public ResponseDTO upload(FileUploadFormRequest form, String username) {
        try {
            Optional<User> optionalUser = userRepository.findByUsername(username);
            if (optionalUser.isEmpty()) {
                return new ResponseDTO(false, "User Not Found");
            }
            Optional<Inspection> optionalInspection = inspectionRepository.findById(form.inspectionId());
            if (optionalInspection.isEmpty()) {
                return new ResponseDTO(false, "Inspection Data Not Found");
            }
            var user= optionalUser.get();
            var inspection= optionalInspection.get();

//            String folder = System.getProperty("jboss.home.dir") + File.separator + "welcome-content";
            String folder = uploadDir;
            String path;
            if(inspection.getMeterNo().isEmpty()){
                path = File.separator + "inspectionFiles";
            }
            else {
                path = File.separator + "inspectionFiles" + File.separator + inspection.getMeterNo();
            }
            folder = folder + path;
            if (form.fileName() == null || form.fileName().isEmpty()) {
                return new ResponseDTO(false, "File Name Can not be empty!");
            }
            if (this.createFolderIfNotExists(folder)) {

                String filePath = path + File.separator + form.fileName();
                String fileName = folder + File.separator + form.fileName().replace("\\s", "_");

                if (this.writeFile(form.data(), fileName)) {
                    InspectionFile inspectionFile = new InspectionFile();
                    inspectionFile.setFileName(filePath);
                    inspectionFile.setFileUploadedOn(new Date());
                    inspectionFile.setUser(user);
                    inspectionFile.setInspection(inspection);
                    inspectionFile.setStatus(statusRepository.findById(1L).get());

                    inspectionFileRepository.save(inspectionFile);

                    TaskHistory taskHistory = new TaskHistory();

                    taskHistory.setActionDate(new Date());
                    taskHistory.setInspection(inspection);
                    taskHistory.setActionBy(user);
                    taskHistory.setActionType("PICTURE UPLOADED");

                    taskHistory.setHistoryDetails(user.getFirstName() + " " + user.getLastName() + " Uploaded Inspection Picture");


                    this.asyncService.postHistory(taskHistory);

                    return new ResponseDTO(true, "File Uploaded Successfully");
                } else {
                    return new ResponseDTO(false, "A file with the same name already exists!");
                }
            } else {
                return new ResponseDTO(false, "Unable to create folder!");
            }

        } catch (Exception var5) {
            log.error(var5.getMessage());
            return new ResponseDTO(false, "File Upload Failed!");
        }
    }

    @Override
    public ResponseDTO updateCustomerInfo(Long inspectionId, UpdateCustomerInfoRequest request, String username) {
        Optional<Inspection> optionalInspection = inspectionRepository.findById(inspectionId);
        if (optionalInspection.isEmpty()) {
            return new ResponseDTO(false, "Inspection not found!");
        }
        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isEmpty()) {
            return new ResponseDTO(false, "User not found!");
        }
        User user = optionalUser.get();
        Inspection inspection = optionalInspection.get();

        // Constructing additional note
        StringBuilder additionalNoteBuilder = new StringBuilder();
        additionalNoteBuilder.append("Updated fields:").append(System.lineSeparator());

        if (!inspection.getCustomerName().equals(request.customerName())) {
            additionalNoteBuilder.append("Customer Name: ").append(inspection.getCustomerName()).append(" -> ").append(request.customerName()).append(System.lineSeparator());
            inspection.setCustomerName(request.customerName());
        }

        if (!inspection.getProblemTypes().equals(request.problemType())) {
            additionalNoteBuilder.append("Problem Types: ").append(inspection.getProblemTypes()).append(" -> ").append(request.problemType()).append(System.lineSeparator());
            inspection.setProblemTypes(request.problemType());
        }

        if (!inspection.getMeterNo().equals(request.meterNumber())) {
            additionalNoteBuilder.append("Meter Number: ").append(inspection.getMeterNo()).append(" -> ").append(request.meterNumber()).append(System.lineSeparator());
            inspection.setMeterNo(request.meterNumber());
        }

        if (!inspection.getPhoneNo().equals(request.phoneNumber())) {
            additionalNoteBuilder.append("Phone Number: ").append(inspection.getPhoneNo()).append(" -> ").append(request.phoneNumber()).append(System.lineSeparator());
            inspection.setPhoneNo(request.phoneNumber());
        }

        inspectionRepository.save(inspection);

        TaskHistory taskHistory = new TaskHistory();
        taskHistory.setInspection(inspection);
        taskHistory.setActionBy(user);
        taskHistory.setActionDate(new Date());
        taskHistory.setActionType("CUSTOMER INFO EDITED");
        taskHistory.setHistoryDetails(user.getFirstName() + " " + user.getLastName() + " Edited Customer Info");
        taskHistory.setAdditionalNote(additionalNoteBuilder.toString());
        this.asyncService.postHistory(taskHistory);

        return new ResponseDTO(true, "Customer Info Updated Successfully!");
    }

    @Override
    public ResponseDTO updateCheckList(List<UpdateCheckList> requestList, String username) {
        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isEmpty()) {
            return new ResponseDTO(false, "User not found!");
        }
        User user = optionalUser.get();
        // Constructing additional note
        StringBuilder additionalNoteBuilder = new StringBuilder();
        additionalNoteBuilder.append("Updated fields:").append(System.lineSeparator());
        Inspection inspection=null;
        for(var request: requestList){
            Optional<CheckListResult> optionalCheckList= checkListResultRepository.findById(request.id());
            if(optionalCheckList.isEmpty()){
                return new ResponseDTO(false,"CheckList not found!");
            }
            var checkList = optionalCheckList.get();
            if(!checkList.isChecklistStatus() == request.status()){
                additionalNoteBuilder.append(checkList.getCheckList().getCheckList()).append(" Status: ").append(checkList.isChecklistStatus()).append(" -> ").append(request.status()).append(System.lineSeparator());
                checkList.setChecklistStatus(request.status());
            }
            if(!checkList.getRemark().equals(request.remark())){
                additionalNoteBuilder.append(checkList.getCheckList().getCheckList()).append(" Remark: ").append(checkList.getRemark()).append(" -> ").append(request.remark()).append(System.lineSeparator());
                checkList.setRemark(request.remark());
            }
            inspection=checkList.getInspection();
            checkList.setUpdatedOn(new Date());
            checkListResultRepository.save(checkList);
        }
        TaskHistory taskHistory = new TaskHistory();
        taskHistory.setInspection(inspection);
        taskHistory.setActionBy(user);
        taskHistory.setActionDate(new Date());
        taskHistory.setActionType("Meter Installation Checklist Edited");
        taskHistory.setHistoryDetails(user.getFirstName() + " " + user.getLastName() + " Meter Installation Checklist");
        taskHistory.setAdditionalNote(additionalNoteBuilder.toString());
        this.asyncService.postHistory(taskHistory);
        return new ResponseDTO(true,"Checklist updated Successfully!");
    }

    @Override
    public ResponseDTO updateCodeResult(Long inspectionId, List<CodeResultUpdateRequest> codeResults, String username) {
        Optional<Inspection> optionalInspection = inspectionRepository.findById(inspectionId);
        if (optionalInspection.isEmpty()) {
            return new ResponseDTO(false, "Inspection not found!");
        }
        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isEmpty()) {
            return new ResponseDTO(false, "User not found!");
        }
        User user = optionalUser.get();
        Inspection inspection = optionalInspection.get();
        Set<CodeResult> codeResultList= inspection.getCodeResults();
        List<Long> requestCodeResultIdList= codeResults.stream().map(CodeResultUpdateRequest::id).toList();
        // Constructing additional note
        StringBuilder additionalNoteBuilder = new StringBuilder();
        additionalNoteBuilder.append("Updated fields:").append(System.lineSeparator());
        for(var codeResult:codeResultList){
            if(!requestCodeResultIdList.contains(codeResult.getId())){
                additionalNoteBuilder.append("Deleted Code Result: ")
                        .append(codeResult.getInspectionCode().getCode())
                        .append(System.lineSeparator());
                codeResultRepository.delete(codeResult);
            }
        }
        for(var request: codeResults){
            if(request.inspectionCodeId()==null){
                return new ResponseDTO(false,"Invalid Inspection Code");
            }
            Optional<InspectionCode> optionalInspectionCode= inspectionCodeRepository.findById(request.inspectionCodeId());
            if(optionalInspectionCode.isEmpty()){
                return new ResponseDTO(false,"Inspection code not found!");
            }
            var inspectionCode= optionalInspectionCode.get();
            CodeResult codeResult;
            if(request.id() != null){
                Optional<CodeResult> optionalCodeResult= codeResultRepository.findById(request.id());
                if(optionalCodeResult.isEmpty()){
                    return new ResponseDTO(false,"Code result not found!");
                }
                codeResult=optionalCodeResult.get();
                if(!codeResult.getResult().equals(request.result())){
                    additionalNoteBuilder.append("Code Result ")
                            .append(codeResult.getInspectionCode().getCode()).append(" :")
                            .append(codeResult.getResult())
                            .append(" -> ")
                            .append(request.result())
                            .append(System.lineSeparator());
                    codeResult.setResult(request.result());
                }

                codeResult.setInspectionCode(inspectionCode);
                codeResult.setUpdatedOn(new Date());
            }
            else{
                codeResult= new CodeResult();
                codeResult.setInspection(inspection);
                codeResult.setInspectionCode(inspectionCode);
                codeResult.setResult(request.result());
                codeResult.setUser(user);
                codeResult.setRegisteredOn(new Date());
                additionalNoteBuilder.append("Add Code Result ")
                        .append(inspectionCode.getCode()).append(" :")
                        .append(codeResult.getResult())
                        .append(System.lineSeparator());
            }
            codeResultRepository.save(codeResult);
        }
        TaskHistory taskHistory = new TaskHistory();
        taskHistory.setInspection(inspection);
        taskHistory.setActionBy(user);
        taskHistory.setActionDate(new Date());
        taskHistory.setActionType("Code List Updated");
        taskHistory.setHistoryDetails(user.getFirstName() + " " + user.getLastName() + " Updated Code List");
        taskHistory.setAdditionalNote(additionalNoteBuilder.toString());
        this.asyncService.postHistory(taskHistory);
        return new ResponseDTO(true,"Code Result updated Successfully");
    }

    @Override
    public ResponseDTO updateRemark(Long inspectionId, UpdateRemarkRequest request, String username) {
        Optional<Inspection> optionalInspection= inspectionRepository.findById(inspectionId);
        if (optionalInspection.isEmpty()) {
            return new ResponseDTO(false, "Inspection not found!");
        }
        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isEmpty()) {
            return new ResponseDTO(false, "User not found!");
        }
        User user = optionalUser.get();
        Inspection inspection = optionalInspection.get();
        StringBuilder additionalNoteBuilder = new StringBuilder();
        additionalNoteBuilder.append("Updated fields:").append(System.lineSeparator());
        if(!inspection.getRemark().equals(request.remark())){
            additionalNoteBuilder.append("Updated Remark: ")
                    .append(inspection.getRemark())
                    .append(" -> ")
                    .append(request.remark());
            inspection.setRemark(request.remark());
            inspectionRepository.save(inspection);
            TaskHistory taskHistory = new TaskHistory();
            taskHistory.setInspection(inspection);
            taskHistory.setActionBy(user);
            taskHistory.setActionDate(new Date());
            taskHistory.setActionType("Remark Updated");
            taskHistory.setHistoryDetails(user.getFirstName() + " " + user.getLastName() + " Updated Code List");
            taskHistory.setAdditionalNote(additionalNoteBuilder.toString());
            this.asyncService.postHistory(taskHistory);
        }
        return new ResponseDTO(true,"Remark updated Successfully");
    }

    @Override
    public List<InspectionCodesResponse> inspectionCodesByProblemTypes(List<Long> problemTypeIds) {
        List<InspectionCode> inspectionCodes=inspectionCodeRepository.findDistinctByProblemTypeIds(problemTypeIds);
        return inspectionCodes.stream().map(inspectionCodeMapper::toResponse).toList();
    }


    private boolean createFolderIfNotExists(String dirName) throws SecurityException {
        File theDir = new File(dirName);
        log.info(dirName);
        if (!theDir.exists()) {
            log.info("the path not exists");
            return theDir.mkdirs();
        } else {
            log.info("the path exists");
            return true;
        }

    }
    private boolean writeFile(MultipartFile content, String filename) throws IOException {
        try {
            File file = new File(filename);
            if (file.exists()) {
                return false;
            }
            content.transferTo(file);
            return true;
        } catch (IOException e) {
            log.error(e.getMessage());
            return false;
        }
    }
}
