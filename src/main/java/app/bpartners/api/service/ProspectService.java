package app.bpartners.api.service;

import static app.bpartners.api.endpoint.rest.model.JobStatusValue.FAILED;
import static app.bpartners.api.endpoint.rest.model.JobStatusValue.FINISHED;
import static app.bpartners.api.endpoint.rest.model.JobStatusValue.IN_PROGRESS;
import static app.bpartners.api.endpoint.rest.model.JobStatusValue.NOT_STARTED;
import static app.bpartners.api.endpoint.rest.model.NewInterventionOption.NEW_PROSPECT;
import static app.bpartners.api.endpoint.rest.model.NewInterventionOption.OLD_CUSTOMER;
import static app.bpartners.api.endpoint.rest.model.ProspectStatus.CONTACTED;
import static app.bpartners.api.endpoint.rest.model.ProspectStatus.CONVERTED;
import static app.bpartners.api.endpoint.rest.model.ProspectStatus.TO_CONTACT;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.repository.expressif.fact.NewIntervention.OldCustomer.OldCustomerType.INDIVIDUAL;
import static app.bpartners.api.repository.google.sheets.SheetConf.GRID_SHEET_TYPE;
import static app.bpartners.api.service.utils.FilterUtils.distinctByKeys;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.SesConf;
import app.bpartners.api.endpoint.event.model.ProspectEvaluationJobInitiated;
import app.bpartners.api.endpoint.event.model.ProspectUpdated;
import app.bpartners.api.endpoint.rest.model.ContactNature;
import app.bpartners.api.endpoint.rest.model.JobStatusValue;
import app.bpartners.api.endpoint.rest.model.NewInterventionOption;
import app.bpartners.api.endpoint.rest.model.ProspectStatus;
import app.bpartners.api.model.Attachment;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.mapper.ProspectMapper;
import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.model.prospect.ProspectStatusHistory;
import app.bpartners.api.model.prospect.job.AntiHarmRules;
import app.bpartners.api.model.prospect.job.ProspectEvaluationJob;
import app.bpartners.api.model.prospect.job.ProspectEvaluationJobRunner;
import app.bpartners.api.repository.ProspectEvaluationJobRepository;
import app.bpartners.api.repository.ProspectRepository;
import app.bpartners.api.repository.expressif.ProspectEvaluation;
import app.bpartners.api.repository.expressif.ProspectEvaluationInfo;
import app.bpartners.api.repository.expressif.ProspectResult;
import app.bpartners.api.repository.expressif.fact.NewIntervention;
import app.bpartners.api.repository.google.calendar.CalendarApi;
import app.bpartners.api.repository.google.sheets.SheetApi;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import app.bpartners.api.repository.jpa.model.HProspectStatusHistory;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.dataprocesser.ProspectDataProcessor;
import app.bpartners.api.service.prospect.ProspectEvaluationJobConverter;
import app.bpartners.api.service.prospect.ProspectResultConverter;
import app.bpartners.api.service.utils.CustomDateFormatter;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.mail.MessagingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;

@Service
@AllArgsConstructor
@Slf4j
public class ProspectService {
  public static final int DEFAULT_RATING_PROSPECT_TO_CONVERT = 8;
  private static final int MAX_DISTANCE_LIMIT = 1_000;
  private static final String PROSPECT_RELAUNCH_TEMPLATE = "prospect_relaunch_template";
  private final ProspectRepository repository;
  private final ProspectDataProcessor dataProcessor;
  private final AccountHolderJpaRepository accountHolderJpaRepository;
  private final SesService sesService;
  private final CustomerService customerService;
  private final SheetApi sheetApi;
  private final ProspectMapper prospectMapper;
  private final ProspectEvaluationJobRepository evaluationJobRepository;
  private final EventProducer eventProducer;
  private final SesConf sesConf;
  private final ProspectStatusService statusService;
  private final SnsService snsService;
  private final UserService userService;
  private final CalendarApi calendarApi;
  private final TemplateResolverEngine templateResolverEngine;
  private final CustomDateFormatter customDateFormatter;
  private final ProspectResultConverter prospectResultConverter;
  private final ProspectEvaluationJobConverter evaluationJobConverter;

  private List<ProspectResult> retrieveCustomersOnly(List<ProspectResult> prospectResults) {
    return prospectResults.stream()
        .filter(result -> result.getCustomerInterventionResult() != null)
        .collect(Collectors.toList());
  }

  private List<ProspectResult> retrieveNewProspects(List<ProspectResult> prospectResults) {
    return prospectResults.stream()
        .filter(
            result ->
                result.getInterventionResult() != null
                    && result.getCustomerInterventionResult() == null)
        .collect(Collectors.toList());
  }

  private HashMap<String, List<ProspectEvaluation>> ownerHashMap(String accountHolderId) {
    HashMap<String, List<ProspectEvaluation>> ownerHasMap = new HashMap<>();
    ownerHasMap.put(accountHolderId, new ArrayList<>());
    return ownerHasMap;
  }

  public static List<ProspectStatusHistory> defaultStatusHistory() {
    return List.of(
        ProspectStatusHistory.builder().status(TO_CONTACT).updatedAt(Instant.now()).build());
  }

  public static List<HProspectStatusHistory> defaultStatusHistoryEntity() {
    return List.of(
        HProspectStatusHistory.builder()
            .id(String.valueOf(randomUUID()))
            .status(ProspectStatus.TO_CONTACT)
            .updatedAt(Instant.now())
            .build());
  }

  private List<ProspectResult> removeDuplications(List<ProspectResult> prospectResults) {
    var withoutDuplication = new ArrayList<ProspectResult>();
    var seen = new HashSet<String>();

    for (ProspectResult prospectResult : prospectResults) {
      var prospectEvaluation = prospectResult.getProspectEval();
      var evaluationInfo = prospectEvaluation.getEvaluationInfo();
      var customerResult = prospectResult.getCustomerInterventionResult();
      var key = retrieveKey(customerResult, evaluationInfo);
      if (!seen.contains(key)) {
        seen.add(key);
        withoutDuplication.add(prospectResult);
      }
    }
    return withoutDuplication;
  }

  @NotNull
  private String retrieveKey(
      ProspectResult.CustomerInterventionResult customerResult,
      ProspectEvaluationInfo evaluationInfo) {
    var customer = customerResult == null ? null : customerResult.getOldCustomer();
    var prospectName = customer == null ? evaluationInfo.getName() : customer.getFullName();
    var prospectEmail = customer == null ? evaluationInfo.getEmail() : customer.getEmail();
    var prospectPhone = customer == null ? evaluationInfo.getPhoneNumber() : customer.getPhone();
    var prospectAddress =
        customer == null ? evaluationInfo.getAddress() : customer.getFullAddress();
    return prospectName + ":" + prospectEmail + ":" + prospectPhone + ":" + prospectAddress;
  }

  private String prospectRelaunchEmailBody(List<Prospect> prospects, HAccountHolder accountHolder) {
    Context context = new Context();
    context.setVariable("accountHolder", accountHolder);
    context.setVariable("prospects", prospects);
    return templateResolverEngine.parseTemplateResolver(PROSPECT_RELAUNCH_TEMPLATE, context);
  }

  @Transactional
  public Prospect getById(String id) {
    return repository.getById(id);
  }

  @Transactional
  public List<ProspectEvaluationJob> getEvaluationJobs(
      String idAccountHolder, List<JobStatusValue> statuses) {
    if (statuses == null) {
      statuses = List.of(NOT_STARTED, IN_PROGRESS, FINISHED, FAILED);
    }
    return evaluationJobRepository.findAllByIdAccountHolderAndStatusesIn(idAccountHolder, statuses);
  }

  @Transactional
  public ProspectEvaluationJob getEvaluationJob(String jobId) {
    return evaluationJobRepository.getById(jobId);
  }

  public List<Prospect> getByCriteria(
      String idAccountHolder,
      String name,
      String contactNatureValue,
      String statusValue,
      PageFromOne page,
      BoundedPageSize pageSize) {
    ContactNature contactNature;
    ProspectStatus prospectStatus;
    int pageValue = page != null ? page.getValue() - 1 : 0;
    int pageSizeValue = pageSize != null ? pageSize.getValue() : 30;
    try {
      contactNature = contactNatureValue == null ? null : ContactNature.valueOf(contactNatureValue);
      prospectStatus = statusValue == null ? null : ProspectStatus.valueOf(statusValue);
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("Unknown contactNature type = " + contactNatureValue);
    }
    String nameValue = name == null ? "" : name;
    return dataProcessor.processProspects(
        repository.findAllByIdAccountHolder(
            idAccountHolder, nameValue, contactNature, prospectStatus, pageValue, pageSizeValue));
  }

  @Transactional
  public List<Prospect> saveAll(List<Prospect> toCreate) {
    List<Prospect> savedProspects = repository.saveAll(toCreate);

    savedProspects.forEach(
        savedProspect -> eventProducer.accept(List.of(toTypedEvent(savedProspect))));

    return savedProspects;
  }

  private ProspectUpdated toTypedEvent(Prospect prospect) {
    return ProspectUpdated.builder().prospect(prospect).updatedAt(Instant.now()).build();
  }

  @Transactional
  public Prospect update(Prospect toSave) {
    Prospect existing = repository.getById(toSave.getId());
    if (existing == null) {
      throw new NotFoundException("Prospect(id=" + toSave.getId() + ") not found");
    }
    // validateStatusUpdateFlow(toSave, existing);
    Prospect savedProspect = repository.save(toSave);

    eventProducer.accept(
        List.of(
            ProspectUpdated.builder().prospect(savedProspect).updatedAt(Instant.now()).build()));

    return savedProspect;
  }

  private void validateStatusUpdateFlow(Prospect toSave, Prospect existing) {
    StringBuilder exceptionBuilder = new StringBuilder();
    if (toSave.getActualStatus() == TO_CONTACT && existing.getActualStatus() != CONTACTED) {
      exceptionBuilder
          .append("Prospect(id=")
          .append(toSave.getId())
          .append(",status=")
          .append(toSave.getActualStatus())
          .append(") can only be updated to status ")
          .append(CONTACTED);
    } else if (toSave.getActualStatus() == CONTACTED && existing.getActualStatus() != CONVERTED) {
      exceptionBuilder
          .append("Prospect(id=")
          .append(toSave.getId())
          .append(",status=")
          .append(toSave.getActualStatus())
          .append(") can only be updated to status ")
          .append(CONVERTED);
    }
    String errorMsg = exceptionBuilder.toString();
    if (!errorMsg.isEmpty()) {
      throw new BadRequestException(errorMsg);
    }
  }

  public List<ProspectEvaluationJob> runEvaluationJobs(
      String userId, String accountHolderId, List<ProspectEvaluationJobRunner> jobRunners) {
    Optional<ProspectEvaluationJobRunner> anyEventConversionJob =
        jobRunners.stream().filter(ProspectEvaluationJobRunner::isEventConversionJob).findAny();
    if (anyEventConversionJob.isPresent() && !calendarApi.hasValidToken(userId)) {
      throw new BadRequestException(
          "CALENDAR_EVENT_CONVERSION job is to be executed "
              + "but calendar access token is expired or invalid");
    }
    List<ProspectEvaluationJob> jobs =
        jobRunners.stream()
            .map(jobRunner -> evaluationJobConverter.convert(accountHolderId, jobRunner))
            .collect(Collectors.toList());

    List<ProspectEvaluationJob> savedJobs = evaluationJobRepository.saveAll(jobs);

    eventProducer.accept(
        jobRunners.stream()
            .map(evaluationJobRunner -> toTypedEvent(evaluationJobRunner, userId))
            .collect(Collectors.toList()));

    return savedJobs;
  }

  private ProspectEvaluationJobInitiated toTypedEvent(
      ProspectEvaluationJobRunner evaluationJobRunner, String userId) {
    return ProspectEvaluationJobInitiated.builder()
        .jobId(evaluationJobRunner.getJobId())
        .idUser(userId)
        .jobRunner(evaluationJobRunner)
        .build();
  }

  @Transactional
  public List<ProspectEvaluationJob> saveEvaluationJobs(
      List<ProspectEvaluationJob> evaluationJobs) {
    return evaluationJobRepository.saveAll(evaluationJobs);
  }

  @Transactional
  public List<ProspectResult> evaluateProspects(
      String accountHolderId,
      AntiHarmRules antiHarmRules,
      List<ProspectEvaluation> prospectsToEvaluate,
      NewInterventionOption interventionOption,
      Double minProspectRating,
      Double minCustomerRating) {
    var resultRecord =
        computeProspectEvaluationResult(
            accountHolderId, antiHarmRules, prospectsToEvaluate, interventionOption);
    return getProspectResults(
        interventionOption, minProspectRating, minCustomerRating, resultRecord.prospectResults);
  }

  // TODO: IMPORTANT ! Only NewIntervention rule is supported for now
  @Transactional
  public List<ProspectResult> evaluateAndSaveProspects(
      String accountHolderId,
      AntiHarmRules antiHarmRules,
      List<ProspectEvaluation> prospectsToEvaluate,
      NewInterventionOption interventionOption,
      Double minProspectRating,
      Double minCustomerRating) {
    var resultRecord =
        computeProspectEvaluationResult(
            accountHolderId, antiHarmRules, prospectsToEvaluate, interventionOption);
    boolean isOldCustomer = interventionOption == OLD_CUSTOMER;
    var newProspects =
        isOldCustomer
            ? new ArrayList<Prospect>()
            : retrieveProspects(resultRecord.prospectResults(), minProspectRating);
    var oldCustomerProspects =
        resultRecord.isNotNewProspect()
            ? retrieveOldCustomers(resultRecord.prospectResults(), minCustomerRating)
            : new ArrayList<Prospect>();
    var prospectsToSave =
        Stream.of(newProspects, oldCustomerProspects).flatMap(List::stream).toList();
    var prospectsWithoutDuplication =
        prospectsToSave.stream()
            .filter(
                distinctByKeys(
                    Prospect::getName,
                    Prospect::getEmail,
                    Prospect::getPhone,
                    Prospect::getAddress))
            .collect(Collectors.toList());
    repository.create(prospectsWithoutDuplication);

    return getProspectResults(
        resultRecord.interventionOption(),
        minProspectRating,
        minCustomerRating,
        resultRecord.prospectResults());
  }

  @NotNull
  private ProspectResultRecord computeProspectEvaluationResult(
      String accountHolderId,
      AntiHarmRules antiHarmRules,
      List<ProspectEvaluation> prospectsToEvaluate,
      NewInterventionOption interventionOption) {
    boolean isNotNewProspect =
        (interventionOption == null ? interventionOption = NEW_PROSPECT : interventionOption)
            != NEW_PROSPECT;
    var customersToEvaluate =
        isNotNewProspect
            ? retrieveOldCustomers(accountHolderId, antiHarmRules, prospectsToEvaluate)
            : new ArrayList<ProspectEvaluation>();
    var prospectResults =
        repository.evaluate(
            Stream.of(prospectsToEvaluate, customersToEvaluate).flatMap(List::stream).toList());
    return new ProspectResultRecord(interventionOption, isNotNewProspect, prospectResults);
  }

  private record ProspectResultRecord(
      NewInterventionOption interventionOption,
      boolean isNotNewProspect,
      List<ProspectResult> prospectResults) {}

  private List<ProspectResult> getProspectResults(
      NewInterventionOption option,
      Double minProspectRating,
      Double minCustomerRating,
      List<ProspectResult> prospectResults) {
    var prospectWithoutDuplication = removeDuplications(prospectResults);
    var filteredRatingResults =
        retrieveNewProspectAndCustomerResults(
            minProspectRating, minCustomerRating, prospectWithoutDuplication);
    return switch (option) {
      case OLD_CUSTOMER -> retrieveCustomersOnly(filteredRatingResults);
      case ALL -> filteredRatingResults;
      default -> retrieveNewProspects(filteredRatingResults);
    };
  }

  @NotNull
  private List<ProspectResult> retrieveNewProspectAndCustomerResults(
      Double minProspectRating,
      Double minCustomerRating,
      List<ProspectResult> prospectWithoutDuplication) {
    return prospectWithoutDuplication.stream()
        .filter(
            result ->
                (result.getInterventionResult() != null
                        && result.getInterventionResult().getRating() >= minProspectRating)
                    || (result.getCustomerInterventionResult() != null
                        && result.getCustomerInterventionResult().getRating() >= minCustomerRating))
        .collect(Collectors.toList());
  }

  private List<Prospect> retrieveOldCustomers(
      List<ProspectResult> prospectResults, Double minCustomerRating) {
    var filteredResultsByCustomerRating =
        retrieveCustomersOnly(prospectResults).stream()
            .filter(
                result -> result.getCustomerInterventionResult().getRating() >= minCustomerRating)
            .collect(Collectors.toList());
    return convertToProspects(filteredResultsByCustomerRating);
  }

  private List<Prospect> retrieveProspects(
      List<ProspectResult> prospectResults, Double minProspectRating) {
    return retrieveNewProspects(prospectResults).stream()
        .filter(result -> result.getInterventionResult().getRating() >= minProspectRating)
        .toList()
        .stream()
        .map(prospectResultConverter::fromResultOnly)
        .toList();
  }

  private List<Prospect> convertToProspects(List<ProspectResult> prospectResults) {
    HashMap<String, List<ProspectResult>> groupByCustomer =
        dispatchResultByCustomer(prospectResults);
    List<Prospect> prospects = new ArrayList<>();
    for (Map.Entry<String, List<ProspectResult>> entry : groupByCustomer.entrySet()) {
      String idCustomer = entry.getKey();
      if (idCustomer != null) {
        Customer customer = customerService.getCustomerById(idCustomer);
        List<ProspectResult> subList = entry.getValue();
        for (ProspectResult result : subList) {
          prospects.add(prospectResultConverter.fromOldCustomer(result, customer));
        }
      } else {
        log.info("Prospects results were customer null {}", entry.getValue());
      }
    }
    return prospects;
  }

  private List<ProspectEvaluation> retrieveOldCustomers(
      String accountHolderId,
      AntiHarmRules antiHarmRules,
      List<ProspectEvaluation> prospectEvaluations) {
    var prospectsGroupByOwner =
        prospectEvaluations.isEmpty()
            ? ownerHashMap(accountHolderId)
            : dispatchEvaluationByOwner(prospectEvaluations);

    var retrievedCustomers = new ArrayList<ProspectEvaluation>();
    for (Map.Entry<String, List<ProspectEvaluation>> entry : prospectsGroupByOwner.entrySet()) {
      var id = entry.getKey();
      var subList = entry.getValue();
      var customers = customerService.findByAccountHolderId(id);
      for (ProspectEvaluation newProspectEval : subList) {
        var newIntervention = (NewIntervention) newProspectEval.getDepaRule();
        for (Customer customer : customers) {
          if (customer.getLocation() == null
              || customer.getLocation().getCoordinate() == null
              || customer.getLocation().getCoordinate().getLatitude() == null
              || customer.getLocation().getCoordinate().getLongitude() == null) {
            continue;
          }
          var distance =
              newIntervention
                  .getCoordinate()
                  .getDistanceFrom(customer.getLocation().getCoordinate());
          if (distance < MAX_DISTANCE_LIMIT) {
            var customerBuilder =
                newIntervention.getOldCustomer().toBuilder()
                    .idCustomer(customer.getId())
                    .oldCustomerAddress(customer.getAddress())
                    .distNewIntAndOldCustomer(distance)
                    .build();
            var prospectBuilder =
                newProspectEval.toBuilder()
                    .id(String.valueOf(randomUUID()))
                    .depaRule(newIntervention.toBuilder().oldCustomer(customerBuilder).build());
            if (prospectEvaluations.isEmpty() && antiHarmRules != null) {
              prospectBuilder.particularCustomer(true);
              prospectBuilder.professionalCustomer(false);
              prospectBuilder.insectControl(antiHarmRules.isInsectControl());
              prospectBuilder.disinfection(antiHarmRules.isDisinfection());
              prospectBuilder.ratRemoval(antiHarmRules.isRatRemoval());
              prospectBuilder.depaRule(
                  newIntervention.toBuilder()
                      .oldCustomer(customerBuilder.toBuilder().type(INDIVIDUAL).build())
                      .build());
            }
            /* /!\ Because here we only evaluate then save in another step
             * Conversion of customers to new prospect is done here*/
            prospectBuilder.evaluationInfo(
                ProspectEvaluationInfo.builder()
                    .owner(id)
                    .name(customer.getFullName())
                    .managerName(customer.getFullName())
                    .email(customer.getEmail())
                    .phoneNumber(customer.getPhone())
                    .address(customer.getAddress())
                    .city(customer.getCity())
                    .coordinates(customer.getLocation().getCoordinate())
                    .postalCode(String.valueOf(customer.getZipCode()))
                    .contactNature(ProspectEvaluationInfo.ContactNature.OLD_CUSTOMER)
                    .category(
                        "Restaurant") // TODO: deprecated, but for now we will set it by default
                    .subcategory(
                        "Restaurant") // TODO: deprecated, but for now we will set it by default
                    .build());
            retrievedCustomers.add(prospectBuilder.build());
          }
        }
      }
    }
    return retrievedCustomers;
  }

  private HashMap<String, List<ProspectResult>> dispatchResultByCustomer(
      List<ProspectResult> prospects) {
    HashMap<String, List<ProspectResult>> prospectResultMap = new HashMap<>();
    for (ProspectResult result : prospects) {
      String idCustomer = result.getCustomerInterventionResult().getIdCustomer();
      if (idCustomer != null) {
        if (!prospectResultMap.containsKey(idCustomer)) {
          List<ProspectResult> subList = new ArrayList<>();
          subList.add(result);
          prospectResultMap.put(idCustomer, subList);
        } else {
          prospectResultMap.get(idCustomer).add(result);
        }
      }
    }
    return prospectResultMap;
  }

  private HashMap<String, List<ProspectEvaluation>> dispatchEvaluationByOwner(
      List<ProspectEvaluation> prospectEvaluations) {
    HashMap<String, List<ProspectEvaluation>> prospectMap = new HashMap<>();
    for (ProspectEvaluation prospectEval : prospectEvaluations) {
      if (prospectEval.isNewIntervention()) {
        String accountHolderId = prospectEval.getProspectOwnerId();
        if (!prospectMap.containsKey(accountHolderId)) {
          List<ProspectEvaluation> subList = new ArrayList<>();
          subList.add(prospectEval);
          prospectMap.put(accountHolderId, subList);
        } else {
          prospectMap.get(accountHolderId).add(prospectEval);
        }
      }
    }
    return prospectMap;
  }

  @Transactional
  public List<ProspectEvaluation> readEvaluationsFromSheets(
      String idUser,
      String ownerId,
      String spreadsheetName,
      String sheetName,
      Integer minRange,
      Integer maxRange) {
    return readEvaluationsFromSheets(idUser, spreadsheetName, sheetName, minRange, maxRange)
        .stream()
        .filter(prospect -> prospect.getProspectOwnerId().equals(ownerId))
        .collect(Collectors.toList());
  }

  // TODO: must be inside repository
  public List<ProspectEvaluation> readEvaluationsFromSheets(
      String idUser, String spreadsheetName, String sheetName, Integer minRange, Integer maxRange) {
    Spreadsheet spreadsheet =
        sheetApi.getSpreadsheetByNames(idUser, spreadsheetName, sheetName, minRange, maxRange);
    List<Sheet> sheets = spreadsheet.getSheets();
    String sheetNameNotFoundMsg =
        "Sheet(name="
            + sheetName
            + ")"
            + " inside Spreadsheet(name="
            + spreadsheet.getProperties().getTitle()
            + ") does not exist";
    if (sheets.isEmpty()) {
      throw new BadRequestException("Spreadsheet has empty sheets or " + sheetNameNotFoundMsg);
    }
    Sheet sheet =
        sheets.stream()
            .filter(s -> s.getProperties().getTitle().equals(sheetName))
            .findAny()
            .orElseThrow(() -> new NotFoundException(sheetNameNotFoundMsg));
    if (!sheet.getProperties().getSheetType().equals(GRID_SHEET_TYPE)) {
      throw new NotImplementedException("Only GRID sheet type is supported");
    }
    return prospectMapper.toProspectEval(sheet);
  }

  // TODO: remove duplication
  public List<ProspectEvaluation> readEvaluationsFromSheetsWithoutFilter(
      String idUser,
      String ownerId,
      String spreadsheetName,
      String sheetName,
      Integer minRange,
      Integer maxRange) {
    Spreadsheet spreadsheet =
        sheetApi.getSpreadsheetByNames(idUser, spreadsheetName, sheetName, minRange, maxRange);
    List<Sheet> sheets = spreadsheet.getSheets();
    String sheetNameNotFoundMsg =
        "Sheet(name="
            + sheetName
            + ")"
            + " inside Spreadsheet(name="
            + spreadsheet.getProperties().getTitle()
            + ") does not exist";
    if (sheets.isEmpty()) {
      String errorMsg = "Spreadsheet has empty sheets or " + sheetNameNotFoundMsg;
      log.error(errorMsg);
      throw new BadRequestException(errorMsg);
    }
    Sheet sheet =
        sheets.stream()
            .filter(s -> s.getProperties().getTitle().equals(sheetName))
            .findAny()
            .orElseThrow(() -> new NotFoundException(sheetNameNotFoundMsg));
    if (!sheet.getProperties().getSheetType().equals(GRID_SHEET_TYPE)) {
      throw new NotImplementedException("Only GRID sheet type is supported");
    }
    return prospectMapper.toProspectEval(ownerId, sheet);
  }

  public List<ProspectEvaluationInfo> readFromSheets(
      String idUser, String spreadsheetName, String sheetName, String artisanOwner) {
    return readFromSheets(idUser, spreadsheetName, sheetName).stream()
        .filter(prospect -> prospect.getOwner() != null && prospect.getOwner().equals(artisanOwner))
        .collect(Collectors.toList());
  }

  // TODO: must be inside repository
  public List<ProspectEvaluationInfo> readFromSheets(
      String idUser, String spreadsheetName, String sheetName) {
    int minRange = 2;
    int maxRange = 100;
    Spreadsheet spreadsheet =
        sheetApi.getSpreadsheetByNames(idUser, spreadsheetName, sheetName, minRange, maxRange);
    List<Sheet> sheets = spreadsheet.getSheets();
    if (sheets.isEmpty()) {
      throw new BadRequestException("Spreadsheet has empty sheets");
    }
    Sheet sheet =
        sheets.stream()
            .filter(s -> s.getProperties().getTitle().equals(sheetName))
            .findAny()
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Sheet(name="
                            + sheetName
                            + ")"
                            + " inside Spreadsheet(name="
                            + spreadsheet.getProperties().getTitle()
                            + ") does not exist"));
    if (!sheet.getProperties().getSheetType().equals(GRID_SHEET_TYPE)) {
      throw new NotImplementedException("Only GRID sheet type is supported");
    }
    return prospectMapper.toProspectEvalInfo(sheet);
  }

  public List<Prospect> importProspectsFromSpreadsheet(
      String idUser, String spreadsheetName, String sheetName, Integer minRange, Integer maxRange) {
    var prospectEvaluations =
        readEvaluationsFromSheets(idUser, spreadsheetName, sheetName, minRange, maxRange);
    var retrievedProspects =
        prospectEvaluations.stream()
            .map(
                eval -> {
                  ProspectEvaluationInfo prospectInfo = eval.getEvaluationInfo();
                  return prospectResultConverter.fromEvaluationAttributes(
                      null, null, prospectInfo, null);
                })
            .collect(Collectors.toList());

    return repository.create(retrievedProspects);
  }

  public void relaunchHoldersProspects() {
    List<Prospect> prospectToContact =
        statusService.findAllByStatus(TO_CONTACT).stream()
            .filter(
                prospect ->
                    prospect.getRating() != null
                        && prospect.getRating().getValue() != null
                        && prospect.getRating().getValue() > 0)
            .collect(Collectors.toList());
    Map<String, List<Prospect>> prospectsByHolder = dispatchByHolder(prospectToContact);
    StringBuilder msgBuilder = new StringBuilder();
    prospectsByHolder.forEach(
        (idHolder, prospects) -> {
          Optional<HAccountHolder> optionalHolder = accountHolderJpaRepository.findById(idHolder);
          if (optionalHolder.isEmpty()) {
            msgBuilder
                .append("Failed to attempt to relaunch AccountHolder(id=")
                .append(idHolder)
                .append(") because it was not found");
          } else {
            try {
              HAccountHolder accountHolder = optionalHolder.get();
              User user = userService.getUserById(accountHolder.getIdUser());
              sendEmailProspectToContact(prospects, optionalHolder);
              notifyProspectsToContact(user);
            } catch (IOException | MessagingException e) {
              throw new ApiException(SERVER_EXCEPTION, e);
            }
          }
        });
    String exceptionMsg = msgBuilder.toString();
    if (!exceptionMsg.isEmpty()) {
      log.warn(exceptionMsg);
    }
  }

  private void sendEmailProspectToContact(
      List<Prospect> prospects, Optional<HAccountHolder> optionalHolder)
      throws IOException, MessagingException {
    HAccountHolder accountHolder = optionalHolder.get();
    String recipient = accountHolder.getEmail();
    String cc = sesConf.getAdminEmail();
    String today = customDateFormatter.formatFrenchDate(Instant.now());
    String emailSubject =
        String.format(
            "[BPartners] Pensez à modifier le statut de vos prospects pour les conserver - %s",
            today);
    String emailBody = prospectRelaunchEmailBody(prospects, accountHolder);
    List<Attachment> attachments = List.of();

    sesService.sendEmail(recipient, cc, emailSubject, emailBody, attachments);
    log.info("Mail sent to {} after relaunching prospects not contacted", recipient);
  }

  private void notifyProspectsToContact(User user) {
    String message = "Pensez à modifier le statut de vos prospects pour les conserver";
    snsService.pushNotification(message, user);
    log.info("Notifications(message=" + message + ") sent to " + user.getName());
  }

  private Map<String, List<Prospect>> dispatchByHolder(List<Prospect> prospects) {
    Map<String, List<Prospect>> prospectsByHolder = new HashMap<>();
    for (Prospect prospect : prospects) {
      String idHolder = prospect.getIdHolderOwner();
      if (idHolder != null) {
        if (!prospectsByHolder.containsKey(idHolder)) {
          List<Prospect> subList = new ArrayList<>();
          subList.add(prospect);
          prospectsByHolder.put(idHolder, subList);
        } else {
          prospectsByHolder.get(idHolder).add(prospect);
        }
      }
    }
    return prospectsByHolder;
  }
}
