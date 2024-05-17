package org.pucar.dristi.validators;

import net.minidev.json.JSONArray;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.pucar.dristi.config.Configuration;
import org.pucar.dristi.repository.CaseRepository;
import org.pucar.dristi.service.CaseService;
import org.pucar.dristi.service.IndividualService;
import org.pucar.dristi.util.AdvocateUtil;
import org.pucar.dristi.util.FileStoreUtil;
import org.pucar.dristi.util.MdmsUtil;
import org.pucar.dristi.web.models.CaseCriteria;
import org.pucar.dristi.web.models.CaseRequest;
import org.pucar.dristi.web.models.CaseSearchRequest;
import org.pucar.dristi.web.models.CourtCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.pucar.dristi.config.ServiceConstants.*;

@Component
public class CaseRegistrationValidator {
    @Autowired
    private IndividualService individualService;
    @Autowired
    private CaseRepository repository;

    CaseService caseService;
    @Autowired
    private MdmsUtil mdmsUtil;

    @Autowired
    private FileStoreUtil fileStoreUtil;

    @Autowired
    private AdvocateUtil advocateUtil;

/*  To do validation->
    1. Validate MDMS data
    2. Fetch court department info from HRMS
    3. Validate artifact Ids
*/
@Autowired
public void setCaseService(@Lazy CaseService caseService) {
    this.caseService = caseService;
}

    public void validateCaseRegistration(CaseRequest caseRequest) throws CustomException{

        caseRequest.getCases().forEach(courtCase -> {
            if(ObjectUtils.isEmpty(courtCase.getTenantId()))
                throw new CustomException(CREATE_CASE_ERR, "tenantId is mandatory for creating case");
            if(ObjectUtils.isEmpty(courtCase.getFilingDate()))
                throw new CustomException(CREATE_CASE_ERR, "filingDate is mandatory for creating case");
            if(ObjectUtils.isEmpty(courtCase.getCaseCategory()))
                throw new CustomException(CREATE_CASE_ERR, "caseCategory is mandatory for creating case");
            if(ObjectUtils.isEmpty(courtCase.getStatutesAndSections()))
                throw new CustomException(CREATE_CASE_ERR, "statute and sections is mandatory for creating case");
            if(ObjectUtils.isEmpty(courtCase.getLitigants()))
                throw new CustomException(CREATE_CASE_ERR, "litigants is mandatory for creating case");
            if(ObjectUtils.isEmpty(caseRequest.getRequestInfo().getUserInfo())){
                throw new CustomException(CREATE_CASE_ERR, "user info is mandatory for creating case");
            }
        });
    }

    public Boolean validateApplicationExistence(CourtCase courtCase, RequestInfo requestInfo) {
        List<CourtCase> existingApplications = repository.getApplications(Collections.singletonList(CaseCriteria.builder().filingNumber(courtCase.getFilingNumber()).build()));

        validateExistingApplications(existingApplications);

        Map<String, Map<String, JSONArray>> mdmsData  = mdmsUtil.fetchMdmsData(requestInfo, existingApplications.get(0).getTenantId(), "case", createMasterDetails());
        validateMdmsData(mdmsData);

        validateLitigants(courtCase, requestInfo);
        validateDocuments(courtCase);
        validateRepresentatives(courtCase, requestInfo);
        validateLinkedCases(courtCase, requestInfo);

        return !existingApplications.isEmpty();
    }

    private void validateExistingApplications(List<CourtCase> existingApplications) {
        if(existingApplications.isEmpty()) {
            throw new CustomException(VALIDATION_ERR,"Case Application does not exist");
        }
        if (ObjectUtils.isEmpty(existingApplications.get(0).getTenantId())) {
            throw new CustomException(CREATE_CASE_ERR, "tenantId is mandatory for creating case");
        }
        if (ObjectUtils.isEmpty(existingApplications.get(0).getFilingDate())) {
            throw new CustomException(CREATE_CASE_ERR, "filingDate is mandatory for creating case");
        }
        if (ObjectUtils.isEmpty(existingApplications.get(0).getCaseCategory())) {
            throw new CustomException(CREATE_CASE_ERR, "caseCategory is mandatory for creating case");
        }
        if (ObjectUtils.isEmpty(existingApplications.get(0).getStatutesAndSections())) {
            throw new CustomException(CREATE_CASE_ERR, "statute and sections is mandatory for creating case");
        }
        if (ObjectUtils.isEmpty(existingApplications.get(0).getLitigants())) {
            throw new CustomException(CREATE_CASE_ERR, "litigants is mandatory for creating case");
        }
    }

    private void validateMdmsData(Map<String, Map<String, JSONArray>> mdmsData) {
        if(mdmsData.get("case") == null) {
            throw new CustomException(MDMS_DATA_NOT_FOUND,"MDMS data does not exist");
        }
    }

    private void validateLitigants(CourtCase courtCase, RequestInfo requestInfo) {
        if(!courtCase.getLitigants().isEmpty()) {
            courtCase.getLitigants().forEach(litigant -> {
                if (!individualService.searchIndividual(requestInfo, litigant.getIndividualId())) {
                    throw new CustomException(INDIVIDUAL_NOT_FOUND, "Invalid complainant details");
                }
            });
        }
    }

    private void validateDocuments(CourtCase courtCase) {
        if(courtCase.getDocuments()!= null && !courtCase.getDocuments().isEmpty()) {
            courtCase.getDocuments().forEach(document -> {
                if (!fileStoreUtil.fileStore(courtCase.getTenantId(), document.getFileStore())) {
                    throw new CustomException(INVALID_FILESTORE_ID, "Invalid document details");
                }
            });
        }
    }

    private void validateRepresentatives(CourtCase courtCase, RequestInfo requestInfo) {
        if(courtCase.getRepresentatives() != null && !courtCase.getRepresentatives().isEmpty()) {
            courtCase.getRepresentatives().forEach(rep -> {
                if (!advocateUtil.fetchAdvocateDetails(requestInfo, rep.getAdvocateId())) {
                    throw new CustomException(INVALID_ADVOCATE_ID, "Invalid advocate details");
                }
            });
        }
    }

    private void validateLinkedCases(CourtCase courtCase, RequestInfo requestInfo) {
        if(courtCase.getLinkedCases()!= null && !courtCase.getLinkedCases().isEmpty()) {
            courtCase.getLinkedCases().forEach(linkedCase -> {
                CaseSearchRequest caseSearchRequest = new CaseSearchRequest();
                List<CaseCriteria> caseCriteriaList = new ArrayList<>();
                CaseCriteria caseCriteria = new CaseCriteria();
                caseCriteria.setCaseId(linkedCase.getId().toString());
                caseCriteriaList.add(caseCriteria);
                caseSearchRequest.setRequestInfo(requestInfo);
                caseSearchRequest.setCriteria(caseCriteriaList);
                if (!caseService.searchCases(caseSearchRequest).isEmpty()) {
                    throw new CustomException(INVALID_LINKEDCASE_ID, "Invalid linked case details");
                }
            });
        }
    }


    private List<String> createMasterDetails() {
        List<String> masterList = new ArrayList<>();
        masterList.add("ComplainantType");
        masterList.add("CaseCategory");
        masterList.add("PaymentMode");
        masterList.add("ResolutionMechanism");

        return masterList;
    }


}