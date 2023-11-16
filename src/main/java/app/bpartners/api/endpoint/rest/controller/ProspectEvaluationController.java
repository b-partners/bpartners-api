package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.ProspectJobRestMapper;
import app.bpartners.api.endpoint.rest.mapper.ProspectRestMapper;
import app.bpartners.api.endpoint.rest.model.EvaluatedProspect;
import app.bpartners.api.endpoint.rest.model.ImportProspect;
import app.bpartners.api.endpoint.rest.model.JobStatusValue;
import app.bpartners.api.endpoint.rest.model.NewInterventionOption;
import app.bpartners.api.endpoint.rest.model.Prospect;
import app.bpartners.api.endpoint.rest.model.ProspectEvaluationJobDetails;
import app.bpartners.api.endpoint.rest.model.ProspectEvaluationJobInfo;
import app.bpartners.api.endpoint.rest.model.ProspectEvaluationRules;
import app.bpartners.api.endpoint.rest.model.PutProspectEvaluationJob;
import app.bpartners.api.endpoint.rest.model.RatingProperties;
import app.bpartners.api.endpoint.rest.model.SheetProperties;
import app.bpartners.api.endpoint.rest.model.SheetProspectEvaluation;
import app.bpartners.api.endpoint.rest.model.SheetRange;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.endpoint.rest.validator.ProspectRestValidator;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.prospect.job.ProspectEvaluationJobRunner;
import app.bpartners.api.repository.expressif.ProspectEvaluation;
import app.bpartners.api.repository.expressif.utils.ProspectEvalUtils;
import app.bpartners.api.service.ProspectEvaluationService;
import app.bpartners.api.service.ProspectService;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static app.bpartners.api.endpoint.rest.validator.ProspectRestValidator.XLSX_FILE;
import static app.bpartners.api.endpoint.rest.validator.ProspectRestValidator.XLS_FILE;

@RestController
@AllArgsConstructor
@Slf4j
public class ProspectEvaluationController {
  public static final String NEW_INTERVENTION_OPTION = "newInterventionOption";
  public static final String MIN_CUSTOMER_RATING = "minCustomerRating";
  public static final String MIN_PROSPECT_RATING = "minProspectRating";
  private final ProspectRestMapper mapper;
  private final ProspectJobRestMapper jobRestMapper;
  private final ProspectEvalUtils prospectUtils;
  private final ProspectRestValidator validator;
  private final ProspectService prospectService;
  private final ProspectEvaluationService evaluationService;

  @PostMapping("/accountHolders/{ahId}/prospects/import")
  public List<Prospect> importProspects(@PathVariable String ahId,
                                        @RequestBody ImportProspect importProspect) {
    validator.accept(importProspect);
    SheetProperties sheetProperties = importProspect.getSpreadsheetImport();
    return prospectService.importProspectsFromSpreadsheet(
            sheetProperties.getSpreadsheetName(),
            sheetProperties.getSheetName(),
            sheetProperties.getRanges().getMin(),
            sheetProperties.getRanges().getMax()).stream()
        .map(mapper::toRest)
        .collect(Collectors.toList());
  }

  @PostMapping("/accountHolders/{ahId}/prospects/evaluations")
  public ResponseEntity evaluateProspects(
      @PathVariable("ahId") String accountHolderId,
      @RequestBody(required = false) SheetProspectEvaluation prospectEvaluation,
      @RequestHeader HttpHeaders headers) {
    validator.accept(prospectEvaluation);
    String acceptHeaders = validator.validateAccept(headers.getFirst(HttpHeaders.ACCEPT));
    boolean isExcelFile = acceptHeaders.equals(XLS_FILE) || acceptHeaders.equals(XLSX_FILE);
    ProspectEvaluationRules evaluationRules = prospectEvaluation.getEvaluationRules();
    RatingProperties ratingProperties = prospectEvaluation.getRatingProperties();
    SheetProperties sheetProperties = prospectEvaluation.getSheetProperties();
    SheetRange range = sheetProperties.getRanges();

    List<ProspectEvaluation> prospectEvaluations = evaluationService.readEvaluationsByOwnerId(
        prospectEvaluation.getArtisanOwner(),
        sheetProperties.getSpreadsheetName(),
        sheetProperties.getSheetName(),
        range.getMin(), range.getMax());

    List<EvaluatedProspect> evaluatedProspects =
        evaluationService.evaluateAndSaveProspects(
                accountHolderId,
                null,
                prospectEvaluations,
                evaluationRules.getNewInterventionOption(),
                ratingProperties.getMinProspectRating(),
                ratingProperties.getMinCustomerRating())
            .stream()
            .map(mapper::toRest)
            .collect(Collectors.toList());
    if (isExcelFile) {
      byte[] excelFile =
          ProspectEvalUtils.convertIntoExcel(null, evaluatedProspects);
      return new ResponseEntity<>(excelFile, HttpStatus.OK);
    }
    return new ResponseEntity<>(evaluatedProspects, HttpStatus.OK);
  }

  @PostMapping("/accountHolders/{ahId}/prospects/prospectsEvaluation")
  public ResponseEntity evaluateProspectsFromExcel(
      @PathVariable("ahId") String accountHolderId,
      @RequestBody byte[] toEvaluate,
      @RequestHeader HttpHeaders headers) {
    log.warn("POST /accountHolders/{ahId}/prospects/prospectsEvaluation is depreacted");
    String acceptHeaders = validator.validateAccept(headers.getFirst(HttpHeaders.ACCEPT));
    NewInterventionOption interventionOption =
        retrieveFromHeader(headers.getFirst(NEW_INTERVENTION_OPTION));
    Double minCustomerRating = extractMinCustomerRating(headers);
    Double minProspectRating = extractMinProspectRating(headers);
    boolean isExcelFile = acceptHeaders.equals(XLS_FILE) || acceptHeaders.equals(XLSX_FILE);

    List<ProspectEvaluation> prospectEvaluations =
        prospectUtils.convertFromExcel(new ByteArrayInputStream(toEvaluate));
    List<EvaluatedProspect> evaluatedProspects =
        evaluationService.evaluateAndSaveProspects(
                accountHolderId,
                null,
                prospectEvaluations,
                interventionOption,
                minProspectRating, minCustomerRating)
            .stream()
            .map(mapper::toRest)
            .collect(Collectors.toList());
    if (isExcelFile) {
      return new ResponseEntity<>(
          ProspectEvalUtils.convertIntoExcel(new ByteArrayInputStream(toEvaluate),
              evaluatedProspects),
          HttpStatus.OK);
    }
    return new ResponseEntity<>(evaluatedProspects, HttpStatus.OK);
  }

  @GetMapping("/accountHolders/{ahId}/prospects/evaluationJobs")
  public List<ProspectEvaluationJobInfo> getProspectEvaluationJobs(@PathVariable String ahId,
                                                                   @RequestParam(name = "statuses", required = false)
                                                                   List<JobStatusValue> statuses) {
    return evaluationService.getEvaluationJobs(ahId, statuses).stream()
        .map(mapper::toRest)
        .collect(Collectors.toList());
  }

  @GetMapping("/accountHolders/{ahId}/prospects/evaluationJobs/{jId}")
  public ProspectEvaluationJobDetails getProspectEvaluationJobDetailsById(@PathVariable String ahId,
                                                                          @PathVariable
                                                                          String jId) {
    return mapper.toRestResult(evaluationService.getEvaluationJob(jId));
  }

  @PutMapping("/accountHolders/{ahId}/prospects/evaluationJobs")
  public List<ProspectEvaluationJobDetails> runProspectEvaluationJobs(@PathVariable String ahId,
                                                                      @RequestBody
                                                                      List<PutProspectEvaluationJob> evaluationJobs) {
    List<ProspectEvaluationJobRunner> jobRunners = evaluationJobs.stream()
        .map(job -> jobRestMapper.toDomain(ahId, job))
        .collect(Collectors.toList());
    return evaluationService.runEvaluationJobs(AuthProvider.getAuthenticatedUserId(), ahId,
            jobRunners)
        .stream()
        .map(mapper::toRestResult)
        .collect(Collectors.toList());
  }

  private Double extractMinCustomerRating(HttpHeaders headers) {
    try {
      double minCustomerRating = Double.parseDouble(headers.getFirst(MIN_CUSTOMER_RATING));
      if (minCustomerRating < 0 || minCustomerRating > 10) {
        throw new BadRequestException("Minimum customer rating value must be between 1 and 10,"
            + " otherwise given is " + minCustomerRating);
      }
      return minCustomerRating;
    } catch (NumberFormatException | NullPointerException e) {
      return Double.valueOf(ProspectService.DEFAULT_RATING_PROSPECT_TO_CONVERT);
    }
  }

  private Double extractMinProspectRating(HttpHeaders headers) {
    try {
      double minProspectRating = Double.valueOf(headers.getFirst(MIN_PROSPECT_RATING));
      if (minProspectRating < 0 || minProspectRating > 10) {
        throw new BadRequestException("Minimum prospect rating value must be between 1 and 10,"
            + " otherwise given is " + minProspectRating);
      }
      return minProspectRating;
    } catch (NumberFormatException | NullPointerException e) {
      return Double.valueOf(ProspectService.DEFAULT_RATING_PROSPECT_TO_CONVERT);
    }
  }

  private NewInterventionOption retrieveFromHeader(String newInterventionOptHeader) {
    if (newInterventionOptHeader == null) {
      return null;
    }
    switch (newInterventionOptHeader) {
      case "ALL":
        return NewInterventionOption.ALL;
      case "NEW_PROSPECT":
        return NewInterventionOption.NEW_PROSPECT;
      case "OLD_CUSTOMER":
        return NewInterventionOption.OLD_CUSTOMER;
      default:
        return null;
    }
  }

}