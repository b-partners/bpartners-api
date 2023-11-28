package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.SesConf;
import app.bpartners.api.endpoint.event.gen.ProspectEvaluationJobInitiated;
import app.bpartners.api.endpoint.event.gen.ProspectUpdated;
import app.bpartners.api.endpoint.rest.model.ContactNature;
import app.bpartners.api.endpoint.rest.model.Geojson;
import app.bpartners.api.endpoint.rest.model.JobStatusValue;
import app.bpartners.api.endpoint.rest.model.NewInterventionOption;
import app.bpartners.api.endpoint.rest.model.ProspectEvaluationJobStatus;
import app.bpartners.api.endpoint.rest.model.ProspectEvaluationJobType;
import app.bpartners.api.endpoint.rest.model.ProspectStatus;
import app.bpartners.api.model.Attachment;
import app.bpartners.api.model.Customer;
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
import app.bpartners.api.repository.expressif.ProspectEval;
import app.bpartners.api.repository.expressif.ProspectEvalInfo;
import app.bpartners.api.repository.expressif.ProspectResult;
import app.bpartners.api.repository.expressif.fact.NewIntervention;
import app.bpartners.api.repository.google.sheets.SheetApi;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import app.bpartners.api.repository.jpa.model.HProspectStatusHistory;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.dataprocesser.ProspectDataProcesser;
import app.bpartners.api.service.utils.DateUtils;
import app.bpartners.api.service.utils.GeoUtils;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.mail.MessagingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;

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
import static app.bpartners.api.model.mapper.CalendarEventMapper.PARIS_TIMEZONE;
import static app.bpartners.api.repository.expressif.fact.NewIntervention.OldCustomer.OldCustomerType.INDIVIDUAL;
import static app.bpartners.api.repository.google.sheets.SheetConf.GRID_SHEET_TYPE;
import static app.bpartners.api.service.utils.FilterUtils.distinctByKeys;
import static app.bpartners.api.service.utils.TemplateResolverUtils.parseTemplateResolver;
import static java.util.UUID.randomUUID;

@Service
@AllArgsConstructor
@Slf4j
public class ProspectService {
  public static final String PROSPECT_MAIL_TEMPLATE = "prospect_mail";
  public static final int DEFAULT_RATING_PROSPECT_TO_CONVERT = 8;
  public static final int MAX_DISTANCE_LIMIT = 1_000;
  public static final String PROSPECT_RELAUNCH_TEMPLATE = "prospect_relaunch_template";
  private final ProspectRepository repository;
  private final ProspectDataProcesser dataProcesser;
  private final AccountHolderJpaRepository accountHolderJpaRepository;
  private final SesService sesService;
  private final CustomerService customerService;
  private final SheetApi sheetApi;
  private final ProspectMapper prospectMapper;
  private final ProspectEvaluationJobRepository evalJobRepository;
  private final EventProducer eventProducer;
  private final SesConf sesConf;
  private final ProspectStatusService statusService;
  private final SnsService snsService;
  private final UserService userService;

  @Transactional
  public Prospect getById(String id) {
    return repository.getById(id);
  }

  @Transactional
  public List<ProspectEvaluationJob> getEvaluationJobs(
      String idAccountHolder,
      List<JobStatusValue> statuses) {
    if (statuses == null) {
      statuses = List.of(NOT_STARTED, IN_PROGRESS, FINISHED, FAILED);
    }
    return evalJobRepository.findAllByIdAccountHolderAndStatusesIn(idAccountHolder, statuses);
  }

  @Transactional
  public ProspectEvaluationJob getEvaluationJob(String jobId) {
    return evalJobRepository.getById(jobId);
  }

  public List<Prospect> getByCriteria(String idAccountHolder,
                                      String name,
                                      String contactNatureValue) {
    ContactNature contactNature;
    try {
      contactNature = contactNatureValue == null ? null
          : ContactNature.valueOf(contactNatureValue);
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("Unknown contactNature type = " + contactNatureValue);
    }
    String nameValue = name == null ? "" : name;
    return dataProcesser.processProspects(
        repository.findAllByIdAccountHolder(idAccountHolder, nameValue, contactNature));
  }

  @Transactional
  public List<Prospect> saveAll(List<Prospect> toCreate) {
    return repository.saveAll(toCreate);
  }

  @Transactional
  public Prospect update(Prospect toSave) {
    Prospect existing = repository.getById(toSave.getId());
    if (existing == null) {
      throw new NotFoundException("Prospect(id=" + toSave.getId() + ") not found");
    }
    //validateStatusUpdateFlow(toSave, existing);
    Prospect savedProspect = repository.save(toSave);
    if (existing.getActualStatus() != savedProspect.getActualStatus()) {
      eventProducer.accept(List.of(ProspectUpdated.builder()
          .prospect(savedProspect)
          .updatedAt(Instant.now())
          .build()));
    }
    return savedProspect;
  }

  private void validateStatusUpdateFlow(Prospect toSave, Prospect existing) {
    StringBuilder exceptionBuilder = new StringBuilder();
    if (toSave.getActualStatus() == TO_CONTACT && existing.getActualStatus() != CONTACTED) {
      exceptionBuilder.append("Prospect(id=").append(toSave.getId()).append(",status=")
          .append(toSave.getActualStatus()).append(") can only be updated to status ")
          .append(CONTACTED);
    } else if (toSave.getActualStatus() == CONTACTED && existing.getActualStatus() != CONVERTED) {
      exceptionBuilder.append("Prospect(id=").append(toSave.getId()).append(",status=")
          .append(toSave.getActualStatus()).append(") can only be updated to status ")
          .append(CONVERTED);
    }
    String errorMsg = exceptionBuilder.toString();
    if (!errorMsg.isEmpty()) {
      throw new BadRequestException(errorMsg);
    }
  }

  @Scheduled(cron = Scheduled.CRON_DISABLED, zone = "Europe/Paris")
  public void prospect() {
    accountHolderJpaRepository.findAll().forEach(accountHolder -> {
      if (repository.needsProspects(accountHolder.getId(), LocalDate.now())
          && repository.isSogefiProspector(accountHolder.getId())) {
        final String subject = "Avez-vous besoin de nouveaux clients ?";
        final String htmlbody =
            parseTemplateResolver(PROSPECT_MAIL_TEMPLATE, configureProspectContext(accountHolder));
        try {
          log.info("The email should be sent to: " + accountHolder.getEmail());
          sesService.sendEmail(accountHolder.getEmail(), null, subject, htmlbody, List.of());
        } catch (IOException | MessagingException e) {
          throw new ApiException(SERVER_EXCEPTION, e);
        }
      }
    });
  }

  public List<ProspectEvaluationJob> runEvaluationJobs(String userId,
                                                       String ahId,
                                                       List<ProspectEvaluationJobRunner> jobRunners) {
    List<ProspectEvaluationJob> jobs = jobRunners.stream()
        .map(jobRunner -> ProspectEvaluationJob.builder()
            .id(jobRunner.getJobId())
            .metadata(jobRunner.getMetadata())
            .idAccountHolder(ahId)
            .type(getJobType(jobRunner))
            .jobStatus(new ProspectEvaluationJobStatus()
                .value(NOT_STARTED)
                .message(null))
            .startedAt(Instant.now())
            .endedAt(null)
            .results(List.of())
            .build())
        .collect(Collectors.toList());

    List<ProspectEvaluationJob> savedJobs = evalJobRepository.saveAll(jobs);

    eventProducer.accept(jobRunners.stream()
        .map(evaluationJobRunner -> toTypedEvent(evaluationJobRunner, userId))
        .collect(Collectors.toList()));

    return savedJobs;
  }

  private ProspectEvaluationJobInitiated toTypedEvent(ProspectEvaluationJobRunner evaluationJobRunner, String userId) {
    return ProspectEvaluationJobInitiated.builder()
        .jobId(evaluationJobRunner.getJobId())
        .idUser(userId)
        .jobRunner(evaluationJobRunner)
        .build();
  }

  @Transactional
  public List<ProspectEvaluationJob> saveEvaluationJobs(
      List<ProspectEvaluationJob> evaluationJobs) {
    return evalJobRepository.saveAll(evaluationJobs);
  }

  @Transactional
  public List<ProspectResult> evaluateProspects(String ahId,
                                                AntiHarmRules antiHarmRules,
                                                List<ProspectEval> prospectsToEvaluate,
                                                NewInterventionOption option,
                                                Double minProspectRating,
                                                Double minCustomerRating) {
    if (option == null) {
      option = NEW_PROSPECT;
    }
    boolean isNotNewProspect = option != NEW_PROSPECT;
    List<ProspectEval> customersToEvaluate =
        isNotNewProspect ? getOldCustomersToEvaluate(ahId, antiHarmRules, prospectsToEvaluate)
            : new ArrayList<>();
    List<ProspectResult> prospectResults =
        repository.evaluate(mergeEvals(prospectsToEvaluate, customersToEvaluate));
    return getProspectResults(option, minProspectRating, minCustomerRating, prospectResults);
  }


  //TODO: IMPORTANT ! Only NewIntervention rule is supported for now
  @Transactional
  public List<ProspectResult> evaluateAndSaveProspects(String ahId,
                                                       AntiHarmRules antiHarmRules,
                                                       List<ProspectEval> prospectsToEvaluate,
                                                       NewInterventionOption option,
                                                       Double minProspectRating,
                                                       Double minCustomerRating) {
    if (option == null) {
      option = NEW_PROSPECT;
    }
    boolean isNotNewProspect = option != NEW_PROSPECT;
    boolean isOldCustomer = option == OLD_CUSTOMER;

    List<ProspectEval> customersToEvalute =
        isNotNewProspect ? getOldCustomersToEvaluate(ahId, antiHarmRules, prospectsToEvaluate)
            : new ArrayList<>();
    List<ProspectResult> prospectResults =
        repository.evaluate(mergeEvals(prospectsToEvaluate, customersToEvalute));

    List<Prospect> newProspects = isOldCustomer ? new ArrayList<>()
        : retrieveProspects(prospectResults, minProspectRating);
    List<Prospect> oldCustomerProspects =
        isNotNewProspect ? retrieveOldCustomers(prospectResults, minCustomerRating)
            : new ArrayList<>();
    List<Prospect> prospectsToSave = mergeProspects(newProspects, oldCustomerProspects);
    List<Prospect> prospectsWithoutDuplication = prospectsToSave.stream()
        .filter(distinctByKeys(
            Prospect::getName,
            Prospect::getEmail,
            Prospect::getPhone,
            Prospect::getAddress))
        .collect(Collectors.toList());
    repository.create(prospectsWithoutDuplication);

    return getProspectResults(option, minProspectRating, minCustomerRating, prospectResults);
  }

  private List<ProspectResult> getProspectResults(NewInterventionOption option,
                                                  Double minProspectRating,
                                                  Double minCustomerRating,
                                                  List<ProspectResult> prospectResults) {
    List<ProspectResult> prospectWithoutDuplication = removeDuplications(prospectResults);
    List<ProspectResult> filteredRatingResults = prospectWithoutDuplication.stream()
        .filter(result -> (result.getInterventionResult() != null
            && result.getInterventionResult().getRating() >= minProspectRating)
            || (result.getCustomerInterventionResult() != null
            && result.getCustomerInterventionResult().getRating() >= minCustomerRating))
        .collect(Collectors.toList());
    switch (option) {
      case OLD_CUSTOMER:
        return filteredCustomers(filteredRatingResults);
      case ALL:
        return filteredRatingResults;
      default:
        return filteredNewProspects(filteredRatingResults);
    }
  }

  private List<Prospect> retrieveOldCustomers(List<ProspectResult> prospectResults,
                                              Double minCustomerRating) {
    List<ProspectResult> filteredResults = ratedCustomers(prospectResults, minCustomerRating);
    return convertToProspects(filteredResults);
  }

  private static List<ProspectResult> ratedCustomers(
      List<ProspectResult> prospectResults, Double minRating) {
    return filteredCustomers(prospectResults).stream()
        .filter(result -> result.getCustomerInterventionResult().getRating() >= minRating)
        .collect(Collectors.toList());
  }

  private static List<ProspectResult> filteredCustomers(
      List<ProspectResult> prospectResults) {
    return prospectResults.stream()
        .filter(result -> result.getCustomerInterventionResult() != null)
        .collect(Collectors.toList());
  }

  private List<Prospect> retrieveProspects(List<ProspectResult> prospectResults,
                                           Double minProspectRating) {
    return ratedProspects(prospectResults, minProspectRating).stream()
        .map(this::convertNewProspect)
        .collect(Collectors.toList());
  }

  private static List<ProspectResult> ratedProspects(
      List<ProspectResult> prospectResults, Double minRating) {
    return filteredNewProspects(prospectResults).stream()
        .filter(result -> result.getInterventionResult().getRating() >= minRating)
        .collect(Collectors.toList());
  }

  private static List<ProspectResult> filteredNewProspects
      (List<ProspectResult> prospectResults) {
    return prospectResults.stream()
        .filter(result -> result.getInterventionResult() != null
            && result.getCustomerInterventionResult() == null)
        .collect(Collectors.toList());
  }

  private static List<Prospect> mergeProspects(List<Prospect> newProspects,
                                               List<Prospect> oldCustomers) {
    List<Prospect> allProspects = new ArrayList<>(newProspects);
    allProspects.addAll(oldCustomers);
    return allProspects;
  }

  private static List<ProspectEval> mergeEvals(List<ProspectEval> prospectEvals,
                                               List<ProspectEval> oldCustomersEval) {
    List<ProspectEval> allEval = new ArrayList<>(prospectEvals);
    allEval.addAll(oldCustomersEval);
    return allEval;
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
          prospects.add(convertOldCustomer(result, customer));
        }
      } else {
        log.info("Prospects results were customer null {}", entry.getValue());
      }
    }
    return prospects;
  }

  private List<ProspectEval> getOldCustomersToEvaluate(String ahId,
                                                       AntiHarmRules antiHarmRules,
                                                       List<ProspectEval> prospectEvals) {
    HashMap<String, List<ProspectEval>> groupByOwner =
        prospectEvals.isEmpty() ? ownerHashMap(ahId)
            : dispatchEvalByOwner(prospectEvals);
    List<ProspectEval> customersToEvaluate = new ArrayList<>();

    for (Map.Entry<String, List<ProspectEval>> entry : groupByOwner.entrySet()) {
      String accountHolderId = entry.getKey();
      List<ProspectEval> subList = entry.getValue();
      List<Customer> customers = customerService.findByAccountHolderId(accountHolderId);

      for (ProspectEval newProspectEval : subList) {
        NewIntervention newIntervention = (NewIntervention) newProspectEval.getDepaRule();

        for (Customer customer : customers) {
          if (customer.getLocation() == null || customer.getLocation().getCoordinate() == null
              || customer.getLocation().getCoordinate().getLatitude() == null
              || customer.getLocation().getCoordinate().getLongitude() == null) {
            continue;
          }
          Double distance = newIntervention.getCoordinate()
              .getDistanceFrom(customer.getLocation().getCoordinate());
          if (distance < MAX_DISTANCE_LIMIT) {
            NewIntervention.OldCustomer customerBuilder =
                newIntervention.getOldCustomer().toBuilder()
                    .idCustomer(customer.getId())
                    .oldCustomerAddress(customer.getAddress())
                    .distNewIntAndOldCustomer(distance)
                    .build();
            ProspectEval.Builder prospectBuilder = newProspectEval.toBuilder()
                .id(String.valueOf(randomUUID())) //new ID
                .depaRule(newIntervention.toBuilder()
                    .oldCustomer(customerBuilder)
                    .build());
            if (prospectEvals.isEmpty() && antiHarmRules != null) {
              prospectBuilder.particularCustomer(true);
              prospectBuilder.professionalCustomer(false);
              prospectBuilder.insectControl(antiHarmRules.isInsectControl());
              prospectBuilder.disinfection(antiHarmRules.isDisinfection());
              prospectBuilder.ratRemoval(antiHarmRules.isRatRemoval());
              prospectBuilder.depaRule(newIntervention.toBuilder()
                  .oldCustomer(customerBuilder.toBuilder()
                      .type(INDIVIDUAL)
                      .build())
                  .build());
            }
            /* /!\ Because here we only evaluate then save in another step
             * Conversion of customers to new prospect is done here*/
            prospectBuilder.prospectEvalInfo(ProspectEvalInfo.builder()
                .owner(accountHolderId)
                .name(customer.getName())
                .managerName(customer.getName())
                .email(customer.getEmail())
                .phoneNumber(customer.getPhone())
                .address(customer.getAddress())
                .city(customer.getCity())
                .coordinates(customer.getLocation().getCoordinate())
                .postalCode(String.valueOf(customer.getZipCode()))
                .contactNature(ProspectEvalInfo.ContactNature.OLD_CUSTOMER)
                .category("Restaurant") //TODO: deprecated, but for now we will set it by default
                .subcategory("Restaurant") //TODO: deprecated, but for now we will set it by default
                .build());
            customersToEvaluate.add(prospectBuilder.build());
          }
        }
      }
    }
    return customersToEvaluate;
  }

  private static HashMap<String, List<ProspectEval>> ownerHashMap(String ahId) {
    HashMap<String, List<ProspectEval>> ownerHasMap = new HashMap<>();
    ownerHasMap.put(ahId, new ArrayList<>());
    return ownerHasMap;
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


  private HashMap<String, List<ProspectEval>> dispatchEvalByOwner(
      List<ProspectEval> prospectEvals) {
    HashMap<String, List<ProspectEval>> prospectMap = new HashMap<>();
    for (ProspectEval prospectEval : prospectEvals) {
      if (prospectEval.isNewIntervention()) {
        String accountHolderId = prospectEval.getProspectOwnerId();
        if (!prospectMap.containsKey(accountHolderId)) {
          List<ProspectEval> subList = new ArrayList<>();
          subList.add(prospectEval);
          prospectMap.put(accountHolderId, subList);
        } else {
          prospectMap.get(accountHolderId).add(prospectEval);
        }
      }
    }
    return prospectMap;
  }

  public Prospect convertNewProspect(ProspectResult result) {
    ProspectEval eval = result.getProspectEval();
    ProspectEvalInfo info =
        result.getProspectEval().getProspectEvalInfo();
    GeoUtils.Coordinate coordinates = info.getCoordinates();
    return fromEvaluationObj(result, eval, info, coordinates);
  }

  private static Prospect fromEvaluationObj(ProspectResult result,
                                            ProspectEval eval,
                                            ProspectEvalInfo info,
                                            GeoUtils.Coordinate coordinates) {
    Integer townCode;
    try {
      townCode =
          info == null ? null : Integer.valueOf(info.getPostalCode());
    } catch (NumberFormatException e) {
      townCode = null;
    }
    return Prospect.builder()
        .id(String.valueOf(randomUUID())) //TODO: change when prospect eval can be override
        .idHolderOwner(eval == null ? null
            : eval.getProspectOwnerId())
        .name(info == null ? null : info.getName())
        .managerName(info == null ? null : info.getManagerName())
        .email(info == null ? null : info.getEmail())
        .phone(info == null ? null : info.getPhoneNumber())
        .address(info == null ? null : info.getAddress())
        .statusHistories(defaultStatusHistory())
        .townCode(info == null ? null : townCode)
        .defaultComment(info == null ? null
            : info.getDefaultComment())
        .townCode(info == null ? null : Integer.valueOf(info.getPostalCode()))
        .location(new Geojson()
            .latitude(coordinates == null ? null
                : coordinates.getLatitude())
            .longitude(coordinates == null ? null
                : coordinates.getLongitude()))
        .rating(Prospect.ProspectRating.builder()
            .value(result == null ? null : result.getInterventionResult().getRating())
            .lastEvaluationDate(result == null ? null : result.getEvaluationDate())
            .build())
        .build();
  }

  public Prospect convertOldCustomer(ProspectResult result, Customer customer) {
    ProspectEval eval = result.getProspectEval();
    result.getCustomerInterventionResult().setOldCustomer(customer);
    return Prospect.builder()
        .id(String.valueOf(randomUUID())) //TODO: change when prospect eval can be override
        .idHolderOwner(eval.getProspectOwnerId())
        .name(customer.getName())
        .managerName(customer.getName())
        .email(customer.getEmail())
        .phone(customer.getPhone())
        .address(customer.getFullAddress())
        .statusHistories(defaultStatusHistory())
        .townCode(Integer.valueOf(customer.getZipCode()))
        .location(new Geojson()
            .latitude(customer.getLocation().getCoordinate().getLatitude())
            .longitude(customer.getLocation().getCoordinate().getLongitude()))
        .rating(Prospect.ProspectRating.builder()
            .value(result.getInterventionResult().getRating())
            .lastEvaluationDate(result.getEvaluationDate())
            .build())
        .build();
  }

  public static List<ProspectStatusHistory> defaultStatusHistory() {
    return List.of(ProspectStatusHistory.builder()
        .status(TO_CONTACT)
        .updatedAt(Instant.now())
        .build());
  }

  public static List<HProspectStatusHistory> defaultStatusHistoryEntity() {
    return List.of(HProspectStatusHistory.builder()
        .id(String.valueOf(randomUUID()))
        .status(ProspectStatus.TO_CONTACT)
        .updatedAt(Instant.now())
        .build());
  }

  @Transactional
  public List<ProspectEval> readEvaluationsFromSheets(String idUser,
                                                      String ownerId,
                                                      String spreadsheetName,
                                                      String sheetName,
                                                      Integer minRange,
                                                      Integer maxRange) {
    return readEvaluationsFromSheets(
        idUser,
        spreadsheetName,
        sheetName,
        minRange,
        maxRange).stream()
        .filter(prospect -> prospect.getProspectOwnerId().equals(ownerId))
        .collect(Collectors.toList());
  }

  //TODO: must be inside repository
  public List<ProspectEval> readEvaluationsFromSheets(String idUser,
                                                      String spreadsheetName,
                                                      String sheetName,
                                                      Integer minRange,
                                                      Integer maxRange) {
    Spreadsheet spreadsheet =
        sheetApi.getSpreadsheetByNames(idUser, spreadsheetName, sheetName, minRange, maxRange);
    List<Sheet> sheets = spreadsheet.getSheets();
    String sheetNameNotFoundMsg = "Sheet(name=" + sheetName + ")"
        + " inside Spreadsheet(name=" + spreadsheet.getProperties().getTitle()
        + ") does not exist";
    if (sheets.isEmpty()) {
      throw new BadRequestException("Spreadsheet has empty sheets or " + sheetNameNotFoundMsg);
    }
    Sheet sheet = sheets.stream()
        .filter(s -> s.getProperties().getTitle().equals(sheetName))
        .findAny().orElseThrow(
            () -> new NotFoundException(sheetNameNotFoundMsg));
    if (!sheet.getProperties().getSheetType().equals(GRID_SHEET_TYPE)) {
      throw new NotImplementedException("Only GRID sheet type is supported");
    }
    return prospectMapper.toProspectEval(sheet);
  }

  //TODO: remove duplication
  public List<ProspectEval> readEvaluationsFromSheetsWithoutFilter(String idUser,
                                                                   String ownerId,
                                                                   String spreadsheetName,
                                                                   String sheetName,
                                                                   Integer minRange,
                                                                   Integer maxRange) {
    Spreadsheet spreadsheet =
        sheetApi.getSpreadsheetByNames(idUser, spreadsheetName, sheetName, minRange, maxRange);
    List<Sheet> sheets = spreadsheet.getSheets();
    String sheetNameNotFoundMsg = "Sheet(name=" + sheetName + ")"
        + " inside Spreadsheet(name=" + spreadsheet.getProperties().getTitle()
        + ") does not exist";
    if (sheets.isEmpty()) {
      String errorMsg = "Spreadsheet has empty sheets or " + sheetNameNotFoundMsg;
      log.error(errorMsg);
      throw new BadRequestException(errorMsg);
    }
    Sheet sheet = sheets.stream()
        .filter(s -> s.getProperties().getTitle().equals(sheetName))
        .findAny().orElseThrow(
            () -> new NotFoundException(sheetNameNotFoundMsg));
    if (!sheet.getProperties().getSheetType().equals(GRID_SHEET_TYPE)) {
      throw new NotImplementedException("Only GRID sheet type is supported");
    }
    return prospectMapper.toProspectEval(ownerId, sheet);
  }

  public List<ProspectEvalInfo> readFromSheets(String idUser,
                                               String spreadsheetName,
                                               String sheetName,
                                               String artisanOwner) {
    return readFromSheets(idUser, spreadsheetName, sheetName).stream()
        .filter(prospect -> prospect.getOwner() != null
            && prospect.getOwner().equals(artisanOwner))
        .collect(Collectors.toList());
  }

  //TODO: must be inside repository
  public List<ProspectEvalInfo> readFromSheets(String idUser,
                                               String spreadsheetName,
                                               String sheetName) {
    int minRange = 2;
    int maxRange = 100;
    Spreadsheet spreadsheet =
        sheetApi.getSpreadsheetByNames(idUser, spreadsheetName, sheetName, minRange, maxRange);
    List<Sheet> sheets = spreadsheet.getSheets();
    if (sheets.isEmpty()) {
      throw new BadRequestException("Spreadsheet has empty sheets");
    }
    Sheet sheet = sheets.stream()
        .filter(s -> s.getProperties().getTitle().equals(sheetName))
        .findAny().orElseThrow(
            () -> new NotFoundException("Sheet(name=" + sheetName + ")"
                + " inside Spreadsheet(name=" + spreadsheet.getProperties().getTitle()
                + ") does not exist"));
    if (!sheet.getProperties().getSheetType().equals(GRID_SHEET_TYPE)) {
      throw new NotImplementedException("Only GRID sheet type is supported");
    }
    return prospectMapper.toProspectEvalInfo(sheet);
  }

  private static ProspectEvaluationJobType getJobType(ProspectEvaluationJobRunner job) {
    if (job.isEventConversionJob()) {
      return ProspectEvaluationJobType.CALENDAR_EVENT_CONVERSION;
    } else if (job.isSpreadsheetEvaluationJob()) {
      return ProspectEvaluationJobType.SPREADSHEET_EVALUATION;
    }
    throw new NotImplementedException(
        "Only prospect evaluation job type [CALENDAR_EVENT_CONVERSION and SPREADSHEET_EVALUATION] are supported for now");
  }

  private Context configureProspectContext(HAccountHolder accountHolder) {
    Context context = new Context();
    context.setVariable("accountHolderEntity", accountHolder);
    return context;
  }

  public static List<ProspectResult> removeDuplications(List<ProspectResult> prospectResults) {
    List<ProspectResult> withoutDuplicat = new ArrayList<>();
    Set<String> seen = new HashSet<>();

    for (ProspectResult prospectResult : prospectResults) {
      ProspectEval eval = prospectResult.getProspectEval();
      ProspectEvalInfo info = eval.getProspectEvalInfo();
      ProspectResult.CustomerInterventionResult customerResult =
          prospectResult.getCustomerInterventionResult();
      Customer customerInfo = customerResult == null ? null
          : customerResult.getOldCustomer();
      String prospectName = customerInfo == null ? info.getName()
          : customerInfo.getName();
      String prospectEmail = customerInfo == null ? info.getEmail()
          : customerInfo.getEmail();
      String prospectPhone = customerInfo == null ? info.getPhoneNumber()
          : customerInfo.getPhone();
      String prospectAddress = customerInfo == null ? info.getAddress()
          : customerInfo.getFullAddress();
      String key = prospectName + ":"
          + prospectEmail + ":"
          + prospectPhone + ":"
          + prospectAddress;

      if (!seen.contains(key)) {
        seen.add(key);
        withoutDuplicat.add(prospectResult);
      }
    }
    return withoutDuplicat;
  }

  public List<Prospect> importProspectsFromSpreadsheet(String idUser,
                                                       String spreadsheetName,
                                                       String sheetName,
                                                       Integer minRange,
                                                       Integer maxRange) {
    List<ProspectEval> prospectEvals = readEvaluationsFromSheets(
        idUser,
        spreadsheetName,
        sheetName,
        minRange,
        maxRange);
    List<Prospect> prospectsToSave = prospectEvals.stream()
        .map(eval -> {
          ProspectEvalInfo prospectInfo = eval.getProspectEvalInfo();
          return fromEvaluationObj(null, null, prospectInfo, null);
        }).collect(Collectors.toList());

    return repository.create(prospectsToSave);
  }

  @Scheduled(cron = "0 0 14 ? * FRI", zone = PARIS_TIMEZONE)
  public void relaunchHoldersProspects() {
    List<Prospect> prospectToContact = statusService.findAllByStatus(TO_CONTACT).stream()
        .filter(prospect -> prospect.getRating() != null
            && prospect.getRating().getValue() != null
            && prospect.getRating().getValue() > 0)
        .collect(Collectors.toList());
    Map<String, List<Prospect>> prospectsByHolder = dispatchByHolder(prospectToContact);
    StringBuilder msgBuilder = new StringBuilder();
    prospectsByHolder.forEach(
        (idHolder, prospects) -> {
          Optional<HAccountHolder> optionalHolder = accountHolderJpaRepository.findById(idHolder);
          if (optionalHolder.isEmpty()) {
            msgBuilder.append("Failed to attempt to relaunch AccountHolder(id=")
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
        }
    );
    String exceptionMsg = msgBuilder.toString();
    if (!exceptionMsg.isEmpty()) {
      log.warn(exceptionMsg);
    }
  }

  private void sendEmailProspectToContact(List<Prospect> prospects,
                                          Optional<HAccountHolder> optionalHolder)
      throws IOException, MessagingException {
    HAccountHolder accountHolder = optionalHolder.get();
    String recipient = accountHolder.getEmail();
    String cc = sesConf.getAdminEmail();
    String today = DateUtils.formatFrenchDate(Instant.now());
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

  private static String prospectRelaunchEmailBody(List<Prospect> prospects,
                                                  HAccountHolder accountHolder) {
    Context context = new Context();
    context.setVariable("accountHolder", accountHolder);
    context.setVariable("prospects", prospects);
    return parseTemplateResolver(PROSPECT_RELAUNCH_TEMPLATE, context);
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