package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.ProspectRestMapper;
import app.bpartners.api.endpoint.rest.model.EvaluatedProspect;
import app.bpartners.api.endpoint.rest.model.ExtendedProspectStatus;
import app.bpartners.api.endpoint.rest.model.NewInterventionOption;
import app.bpartners.api.endpoint.rest.model.Prospect;
import app.bpartners.api.endpoint.rest.model.ProspectConversion;
import app.bpartners.api.endpoint.rest.model.ProspectEvaluationRules;
import app.bpartners.api.endpoint.rest.model.RatingProperties;
import app.bpartners.api.endpoint.rest.model.SheetProperties;
import app.bpartners.api.endpoint.rest.model.SheetProspectEvaluation;
import app.bpartners.api.endpoint.rest.model.SheetRange;
import app.bpartners.api.endpoint.rest.model.UpdateProspect;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.endpoint.rest.validator.ProspectRestValidator;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.repository.expressif.ProspectEval;
import app.bpartners.api.repository.expressif.utils.ProspectEvalUtils;
import app.bpartners.api.service.ProspectService;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
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
import static java.util.stream.Collectors.toUnmodifiableList;

@RestController
@AllArgsConstructor
public class ProspectController {
  public static final String NEW_INTERVENTION_OPTION = "newInterventionOption";
  public static final String MIN_CUSTOMER_RATING = "minCustomerRating";
  public static final String MIN_PROSPECT_RATING = "minProspectRating";
  private final ProspectService service;
  private final ProspectRestMapper mapper;
  private final ProspectEvalUtils prospectUtils;
  private final ProspectRestValidator validator;

  private static Double getMinCustomerRating(HttpHeaders headers) {
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

  private static Double getMinProspectRating(HttpHeaders headers) {
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

  private static NewInterventionOption retrieveFromHeader(String newInterventionOptHeader) {
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

  @PutMapping("/accountHolders/{ahId}/prospects/{id}/prospectConversion")
  public List<ProspectConversion> convertProspect(
      @PathVariable("ahId") String accountHolderId, @PathVariable("id") String prospectId,
      @RequestBody List<ProspectConversion> prospectConversion) {
    //TODO: what should we do here ?
    throw new NotImplementedException("prospect conversion not implemented yet");
  }

  @GetMapping("/accountHolders/{ahId}/prospects")
  public List<Prospect> getProspects(@PathVariable("ahId") String accountHolderId,
                                     @RequestParam(name = "name", required = false)
                                     String name) {
    return service.getAllByIdAccountHolder(accountHolderId, name).stream()
        .map(mapper::toRest)
        .collect(toUnmodifiableList());
  }

  @PutMapping("/accountHolders/{ahId}/prospects")
  public List<Prospect> updateProspects(@PathVariable("ahId") String accountHolderId,
                                        @RequestBody List<UpdateProspect> prospects) {
    List<app.bpartners.api.model.Prospect> prospectList = prospects.stream()
        .map(mapper::toDomain)
        .collect(toUnmodifiableList());
    return service.saveAll(prospectList).stream()
        .map(mapper::toRest)
        .collect(toUnmodifiableList());
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

    List<ProspectEval> prospectEvaluations = service.readEvaluationsFromSheets(
        AuthProvider.getAuthenticatedUserId(),
        prospectEvaluation.getArtisanOwner(),
        sheetProperties.getSpreadsheetName(),
        sheetProperties.getSheetName(),
        range.getMin(), range.getMax());

    List<EvaluatedProspect> evaluatedProspects =
        service.evaluateProspects(
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
    String acceptHeaders = validator.validateAccept(headers.getFirst(HttpHeaders.ACCEPT));
    NewInterventionOption interventionOption =
        retrieveFromHeader(headers.getFirst(NEW_INTERVENTION_OPTION));
    Double minCustomerRating = getMinCustomerRating(headers);
    Double minProspectRating = getMinProspectRating(headers);
    boolean isExcelFile = acceptHeaders.equals(XLS_FILE) || acceptHeaders.equals(XLSX_FILE);

    List<ProspectEval> prospectEvals =
        prospectUtils.convertFromExcel(new ByteArrayInputStream(toEvaluate));
    List<EvaluatedProspect> evaluatedProspects =
        service.evaluateProspects(
                prospectEvals, interventionOption, minProspectRating, minCustomerRating)
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

  @PutMapping("/accountHolders/{ahId}/prospects/{id}")
  public Prospect updateProspectsStatus(@PathVariable("ahId") String accountHolderId,
                                        @PathVariable("id") String prospectId,
                                        @RequestBody ExtendedProspectStatus toUpdate) {
    app.bpartners.api.model.Prospect prospect = mapper.toDomain(toUpdate);
    return mapper.toRest(service.save(prospect));
  }
}