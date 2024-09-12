package app.bpartners.api.service.event;

import static app.bpartners.api.endpoint.rest.model.ContactNature.OLD_CUSTOMER;
import static app.bpartners.api.endpoint.rest.model.ContactNature.PROSPECT;
import static app.bpartners.api.endpoint.rest.model.JobStatusValue.FAILED;
import static app.bpartners.api.endpoint.rest.model.JobStatusValue.FINISHED;
import static app.bpartners.api.endpoint.rest.model.JobStatusValue.IN_PROGRESS;
import static app.bpartners.api.endpoint.rest.model.JobStatusValue.NOT_STARTED;
import static app.bpartners.api.model.prospect.job.SheetEvaluationJobRunner.GOLDEN_SOURCE_SPR_SHEET_NAME;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.event.SesConf;
import app.bpartners.api.endpoint.event.model.ProspectEvaluationJobInitiated;
import app.bpartners.api.endpoint.rest.mapper.ProspectRestMapper;
import app.bpartners.api.endpoint.rest.model.CalendarProvider;
import app.bpartners.api.endpoint.rest.model.EvaluatedProspect;
import app.bpartners.api.endpoint.rest.model.Geojson;
import app.bpartners.api.endpoint.rest.model.JobStatusValue;
import app.bpartners.api.endpoint.rest.model.NewInterventionOption;
import app.bpartners.api.endpoint.rest.model.ProspectStatus;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.Attachment;
import app.bpartners.api.model.CalendarEvent;
import app.bpartners.api.model.User;
import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.model.prospect.ProspectStatusHistory;
import app.bpartners.api.model.prospect.job.EventJobRunner;
import app.bpartners.api.model.prospect.job.ProspectEvaluationJob;
import app.bpartners.api.model.prospect.job.ProspectEvaluationJobRunner;
import app.bpartners.api.model.prospect.job.SheetEvaluationJobRunner;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.ban.model.GeoPosition;
import app.bpartners.api.repository.expressif.ProspectEvaluation;
import app.bpartners.api.repository.expressif.ProspectEvaluationInfo;
import app.bpartners.api.repository.expressif.ProspectResult;
import app.bpartners.api.repository.expressif.fact.NewIntervention;
import app.bpartners.api.service.AccountHolderService;
import app.bpartners.api.service.CalendarService;
import app.bpartners.api.service.ProspectService;
import app.bpartners.api.service.SnsService;
import app.bpartners.api.service.UserService;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import app.bpartners.api.service.utils.GeoUtils;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.mail.MessagingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

@Service
@AllArgsConstructor
@Slf4j
public class ProspectEvaluationJobInitiatedService
    implements Consumer<ProspectEvaluationJobInitiated> {
  private static final String JOB_PROCESSING_FAILED_EMAIL_TEMPLATE =
      "prospect_event_evaluation_failed";
  private static final String EVENT_EVALUATION_RESULT_EMAIL_TEMPLATE =
      "prospect_event_evaluation_result";
  private static final String SHEET_EVALUATION_RESULT_EMAIL_TEMPLATE =
      "prospect_sheet_evaluation_result";
  private final AccountHolderService holderService;
  private final ProspectService prospectService;
  private final CalendarService calendarService;
  private final BanApi banApi;
  private final SesService sesService;
  private final SesConf sesConf;
  private final ProspectRestMapper prospectRestMapper;
  private final UserService userService;
  private final SnsService snsService;
  private final TemplateResolverEngine templateResolverEngine;
  private final CustomDateFormatter customDateFormatter;

  @Override
  public void accept(ProspectEvaluationJobInitiated jobInitiated) {
    var job = jobInitiated.getJobRunner();
    ProspectEvaluationJob existingJob = prospectService.getEvaluationJob(job.getJobId());
    ProspectEvaluationJob runningJob = runningJob(existingJob);
    if (runningJob != null && runningJob.getJobStatus().getValue() == IN_PROGRESS) {
      User user = userService.getUserById(jobInitiated.getIdUser());
      var runningHolder = holderService.findDefaultByIdUser(jobInitiated.getIdUser());
      if (job.isEventConversionJob()) {
        handleEventConversionJob(job, runningJob, user, runningHolder);
      } else if (job.isSpreadsheetEvaluationJob()) {
        handleSpreadsheetEvaluationJob(job, runningJob, user, runningHolder);
      } else {
        String exceptionMsg =
            "Only prospect evaluation job types"
                + " CALENDAR_EVENT_CONVERSION and SPREADSHEET_EVALUATION are supported for now";
        updateJobStatus(runningJob, FAILED, exceptionMsg);
        log.warn(exceptionMsg);
      }
    }
  }

  private void handleEventConversionJob(
      ProspectEvaluationJobRunner job,
      ProspectEvaluationJob runningJob,
      User user,
      AccountHolder runningHolder) {
    List<String> locations = null;
    List<CalendarEvent> eventsWithAddress = null;
    try {
      var eventJobRunner = job.getEventJobRunner();
      var ranges = eventJobRunner.getEventDateRanges();
      var antiHarmRules = eventJobRunner.getEvaluationRules().getAntiHarmRules();
      var ratingProperties = eventJobRunner.getRatingProperties();

      eventsWithAddress = getEventWithAddress(user, eventJobRunner, ranges);
      locations = retrieveLocations(eventsWithAddress);
      HashMap<String, List<ProspectEvaluation>> prospectsByEvents =
          getProspectsToEvaluate(user, eventJobRunner, locations);

      for (CalendarEvent calendarEvent : eventsWithAddress) {
        List<ProspectEvaluation> prospects = prospectsByEvents.get(calendarEvent.getLocation());
        List<ProspectResult> evaluatedProspects =
            prospectService.evaluateProspects(
                runningHolder.getId(),
                antiHarmRules,
                prospects,
                NewInterventionOption.ALL,
                ratingProperties.getMinProspectRating(),
                ratingProperties.getMinCustomerRating());

        String interventionDate =
            customDateFormatter.formatFrenchDatetime(calendarEvent.getFrom().toInstant());
        String interventionLocation = calendarEvent.getLocation();
        String emailSubject =
            String.format(
                "Vos prospects à proximité de votre RDV du %s au %s",
                interventionDate, interventionLocation);
        String emailBody =
            eventConversionEmailBody(
                runningHolder, evaluatedProspects, interventionDate, interventionLocation);
        terminateJob(
            user,
            runningJob,
            runningHolder,
            runningHolder,
            evaluatedProspects,
            emailSubject,
            emailBody);
      }
    } catch (Exception e) {
      updateJobStatus(runningJob, FAILED, e.getMessage());
      log.warn(
          "Exception occurred when treating job {} : {}", runningJob.describe(), e.getMessage());
      String recipient = sesConf.getAdminEmail();
      try {
        String concerned = null;
        String runningJobStart =
            customDateFormatter.formatFrenchDatetime(runningJob.getStartedAt());
        String subject =
            "Erreur lors de l'évaluation de prospects de "
                + runningHolder.getName()
                + " lancés le "
                + runningJobStart;
        String htmlBody =
            evaluationJobFailedEmailBody(job, runningHolder, eventsWithAddress, e, runningJobStart);
        sesService.sendEmail(recipient, concerned, subject, htmlBody, List.of());
        log.info(
            "Email sent to email to "
                + recipient
                + " after processing prospect evaluation "
                + runningJob.describe());
      } catch (IOException | MessagingException ex) {
        log.warn(
            "Unable to send email to "
                + recipient
                + " after processing prospect evaluation "
                + runningJob.describe());
      }
    }
  }

  private String evaluationJobFailedEmailBody(
      ProspectEvaluationJobRunner job,
      AccountHolder runningHolder,
      List<CalendarEvent> eventsWithAddress,
      Exception e,
      String runningJobStart) {
    Context context = new Context();
    context.setVariable("accountHolder", runningHolder);
    context.setVariable("job", job);
    context.setVariable("runningJobStart", runningJobStart);
    context.setVariable("events", eventsWithAddress);
    context.setVariable("exception", e);
    return templateResolverEngine.parseTemplateResolver(
        JOB_PROCESSING_FAILED_EMAIL_TEMPLATE, context);
  }

  private List<CalendarEvent> getEventWithAddress(
      User user, EventJobRunner eventJobRunner, EventJobRunner.EventDateRanges ranges) {
    List<CalendarEvent> calendarEvents =
        calendarService.getEvents(
            user.getId(),
            eventJobRunner.getCalendarId(),
            CalendarProvider.GOOGLE_CALENDAR,
            ranges.getFrom(),
            ranges.getTo());
    List<CalendarEvent> eventsWithAddress =
        calendarEvents.stream()
            .filter(event -> event.getLocation() != null)
            .collect(Collectors.toList());
    return eventsWithAddress;
  }

  private void handleSpreadsheetEvaluationJob(
      ProspectEvaluationJobRunner job,
      ProspectEvaluationJob runningJob,
      User user,
      AccountHolder runningHolder) {
    try {
      var sheetJobRunner = job.getSheetJobRunner();
      AccountHolder holderOwner = holderService.getById(sheetJobRunner.getArtisanOwner());
      var evaluationRules = sheetJobRunner.getEvaluationRules();
      var antiHarmRules = evaluationRules.getAntiHarmRules();
      var ratingProperties = sheetJobRunner.getRatingProperties();
      var sheetProperties = sheetJobRunner.getSheetProperties();
      var evaluatedProspects =
          prospectService.evaluateProspects(
              holderOwner.getId(),
              antiHarmRules,
              fromSpreadsheet(user.getId(), sheetJobRunner),
              evaluationRules.getInterventionOption(),
              ratingProperties.getMinProspectRating(),
              ratingProperties.getMinCustomerRating());
      String emailSubject =
          "Les prospects évalués retenus pour "
              + holderOwner.getName()
              + " contenu dans la feuille "
              + sheetProperties.getSheetName();
      String emailBody = spreadsheetEvaluationEmailBody(holderOwner, evaluatedProspects);
      terminateJob(
          user,
          runningJob,
          holderOwner,
          runningHolder,
          evaluatedProspects,
          emailSubject,
          emailBody);
    } catch (Exception e) {
      updateJobStatus(runningJob, FAILED, e.getMessage());
      log.warn(
          "Exception occurred when treating job {} : {}", runningJob.describe(), e.getMessage());
    }
  }

  private void terminateJob(
      User user,
      ProspectEvaluationJob runningJob,
      AccountHolder ownerHolder,
      AccountHolder runningHolder,
      List<ProspectResult> evaluatedProspects,
      String emailSubject,
      String emailBody) {
    long durationMinutes = runningJob.getDuration().toMinutes();
    long durationSeconds = runningJob.getDuration().minusMinutes(durationMinutes).toSeconds();
    List<Prospect> results =
        convertProspectFromResults(runningJob, ownerHolder, evaluatedProspects);
    var finishedJob =
        updateJobStatus(
            runningJob.toBuilder().results(results).build(),
            FINISHED,
            getJobMessage(evaluatedProspects, durationMinutes, durationSeconds));
    if (finishedJob.getJobStatus().getValue() == FINISHED) {
      if (!results.isEmpty()) {
        notifyResultToDevice(results, user);
      }
      sendJobResultThroughEmail(runningHolder, finishedJob, emailSubject, emailBody);
    }
  }

  private void notifyResultToDevice(List<Prospect> prospects, User user) {
    String message =
        prospects.size() == 1
            ? "1 nouveau prospect a été ajouté sur votre dashboard"
            : prospects.size() + " nouveaux prospects ont été ajoutés sur votre dashboard";
    snsService.pushNotification(message, user);
  }

  private String eventConversionEmailBody(
      AccountHolder accountHolder,
      List<ProspectResult> prospectResults,
      String interventionDate,
      String interventionLocation) {
    List<ProspectResult> dispatchedResults =
        prospectResults.stream()
            .filter(
                evalutedProspect ->
                    evalutedProspect
                        .getInterventionResult()
                        .getAddress()
                        .equals(interventionLocation))
            .collect(Collectors.toList());
    List<EvaluatedProspect> evaluatedProspects =
        dispatchedResults.stream().map(prospectRestMapper::toRest).collect(Collectors.toList());
    List<EvaluatedProspect> oldCustomers =
        evaluatedProspects.stream()
            .filter(
                prospect ->
                    prospect.getContactNature() != null
                        && prospect.getContactNature() == OLD_CUSTOMER)
            .collect(Collectors.toList());
    List<EvaluatedProspect> newProspects =
        evaluatedProspects.stream()
            .filter(
                prospect ->
                    prospect.getContactNature() != null && prospect.getContactNature() == PROSPECT)
            .collect(Collectors.toList());

    Context context = new Context();
    context.setVariable("evaluatedProspects", evaluatedProspects);
    context.setVariable("accountHolder", accountHolder);
    context.setVariable("oldCustomers", oldCustomers);
    context.setVariable("newProspects", newProspects);
    context.setVariable("interventionDate", interventionDate);
    context.setVariable("interventionLocation", interventionLocation);
    return templateResolverEngine.parseTemplateResolver(
        EVENT_EVALUATION_RESULT_EMAIL_TEMPLATE, context);
  }

  private String spreadsheetEvaluationEmailBody(
      AccountHolder accountHolder, List<ProspectResult> prospectResults) {
    List<EvaluatedProspect> evaluatedProspects =
        prospectResults.stream().map(prospectRestMapper::toRest).collect(Collectors.toList());
    List<EvaluatedProspect> oldCustomers =
        evaluatedProspects.stream()
            .filter(
                prospect ->
                    prospect.getContactNature() != null
                        && prospect.getContactNature() == OLD_CUSTOMER)
            .collect(Collectors.toList());
    List<EvaluatedProspect> newProspects =
        evaluatedProspects.stream()
            .filter(
                prospect ->
                    prospect.getContactNature() != null && prospect.getContactNature() == PROSPECT)
            .collect(Collectors.toList());

    Context context = new Context();
    context.setVariable("evaluatedProspects", evaluatedProspects);
    context.setVariable("accountHolder", accountHolder);
    context.setVariable("oldCustomers", oldCustomers);
    context.setVariable("newProspects", newProspects);
    return templateResolverEngine.parseTemplateResolver(
        SHEET_EVALUATION_RESULT_EMAIL_TEMPLATE, context);
  }

  private void sendJobResultThroughEmail(
      AccountHolder runningHolder,
      ProspectEvaluationJob finishedJob,
      String emailSubject,
      String emailBody) {
    try {
      String recipient = runningHolder.getEmail();
      String concerned = null;
      String invisibleRecipient = sesConf.getAdminEmail();
      List<Attachment> attachments = List.of();
      sesService.sendEmail(
          recipient, concerned, emailSubject, emailBody, attachments, invisibleRecipient);
      log.info("Job(id=" + finishedJob.getId() + ") " + finishedJob.getJobStatus().getMessage());
    } catch (IOException | MessagingException e) {
      log.warn(
          "Exception occurred when sending mail of job {} : {}",
          finishedJob.describe(),
          e.getMessage());
    }
  }

  private static List<Prospect> convertProspectFromResults(
      ProspectEvaluationJob runningJob,
      AccountHolder accountHolder,
      List<ProspectResult> evaluatedProspects) {
    return evaluatedProspects.stream()
        .map(
            result -> {
              ProspectEvaluationInfo info = result.getProspectEval().getEvaluationInfo();
              var interventionResult = result.getInterventionResult();
              var customerResult = result.getCustomerInterventionResult();
              var ratingBuilder =
                  Prospect.ProspectRating.builder().lastEvaluationDate(Instant.now());
              if (interventionResult != null && customerResult == null) {
                ratingBuilder.value(interventionResult.getRating());
              } else if (interventionResult == null && customerResult != null) {
                ratingBuilder.value(customerResult.getRating());
              }
              Integer townCode;
              try {
                townCode = Integer.valueOf(info.getPostalCode());
              } catch (NumberFormatException e) {
                townCode = null;
              }
              return Prospect.builder()
                  .id(String.valueOf(randomUUID()))
                  .idJob(runningJob.getId())
                  .idHolderOwner(accountHolder.getId())
                  .name(info.getName())
                  .managerName(info.getManagerName())
                  .email(info.getEmail())
                  .phone(info.getPhoneNumber())
                  .location(
                      new Geojson()
                          .longitude(info.getCoordinates().getLongitude())
                          .latitude(info.getCoordinates().getLatitude()))
                  .rating(ratingBuilder.build())
                  .address(info.getAddress())
                  .statusHistories(
                      List.of(
                          ProspectStatusHistory.builder()
                              .status(ProspectStatus.TO_CONTACT)
                              .updatedAt(Instant.now())
                              .build()))
                  .townCode(townCode)
                  .defaultComment(info.getDefaultComment())
                  .comment(null)
                  .contractAmount(null)
                  .prospectFeedback(null)
                  .build();
            })
        .collect(Collectors.toList());
  }

  private static String getJobMessage(
      List<ProspectResult> evaluatedProspects, long durationMinutes, long durationSeconds) {
    return "Successfully processed after "
        + durationMinutes
        + " minutes "
        + durationSeconds
        + " seconds with "
        + evaluatedProspects.size()
        + " evaluated prospects or old customers";
  }

  private ProspectEvaluationJob updateJobStatus(
      ProspectEvaluationJob job, JobStatusValue jobStatusValue, String jobMessage) {
    ProspectEvaluationJob clonedJob =
        job.toBuilder()
            .jobStatus(job.getJobStatus().value(jobStatusValue).message(jobMessage))
            .endedAt(jobStatusValue == FINISHED || jobStatusValue == FAILED ? Instant.now() : null)
            .build();
    return prospectService.saveEvaluationJobs(List.of(clonedJob)).get(0);
  }

  private ProspectEvaluationJob runningJob(ProspectEvaluationJob existingJob) {
    if (existingJob.getJobStatus().getValue() == NOT_STARTED) {
      return updateJobStatus(existingJob, IN_PROGRESS, null);
    }
    return null;
  }

  private HashMap<String, List<ProspectEvaluation>> getProspectsToEvaluate(
      User user, EventJobRunner eventJobRunner, List<String> locations) {
    HashMap<String, List<ProspectEvaluation>> prospectsByEvents = new HashMap<>();
    var newProspects = fromDefaultSheet(user);
    var evaluationRules = eventJobRunner.getEvaluationRules();
    var antiHarmRules = evaluationRules.getAntiHarmRules();
    locations.forEach(
        calendarEventLocation -> {
          List<ProspectEvaluation> subList = new ArrayList<>();
          GeoPosition eventAddressPos = banApi.fSearch(calendarEventLocation);
          GeoUtils.Coordinate eventAddressCoordinates =
              eventAddressPos == null ? null : eventAddressPos.getCoordinates();

          newProspects.forEach(
              prospect -> {
                NewIntervention clonedRule = (NewIntervention) prospect.getDepaRule();
                ProspectEvaluation prospectEval =
                    prospect.toBuilder()
                        .id(String.valueOf(randomUUID()))
                        .prospectOwnerId(eventJobRunner.getArtisanOwner())
                        .ratRemoval(antiHarmRules.isRatRemoval())
                        .disinfection(antiHarmRules.isDisinfection())
                        .insectControl(antiHarmRules.isInsectControl())
                        .depaRule(
                            clonedRule.toBuilder()
                                .newIntAddress(calendarEventLocation)
                                .coordinate(eventAddressCoordinates)
                                .distNewIntAndProspect(
                                    eventAddressCoordinates == null
                                        ? null
                                        : eventAddressCoordinates.getDistanceFrom(
                                            prospect.getEvaluationInfo().getCoordinates()))
                                .build())
                        .build();
                subList.add(prospectEval);
              });
          prospectsByEvents.put(calendarEventLocation, subList);
        });
    return prospectsByEvents;
  }

  private List<String> retrieveLocations(List<CalendarEvent> calendarEvents) {
    return calendarEvents.stream().map(CalendarEvent::getLocation).collect(Collectors.toList());
  }

  private List<ProspectEvaluation> fromDatabase(String idUser, EventJobRunner eventJobRunner) {
    // TODO: retrieve from database here
    return List.of();
  }

  // TODO: use this function to import inside database
  private List<ProspectEvaluation> fromDefaultSheet(User user) {
    AccountHolder accountHolder = user.getDefaultHolder();
    String sheetName = accountHolder.getName();
    int minDefaultRange = 2;
    int maxDefaultRange = 100;
    return prospectService.readEvaluationsFromSheetsWithoutFilter(
        user.getId(),
        accountHolder.getId(),
        GOLDEN_SOURCE_SPR_SHEET_NAME,
        sheetName,
        minDefaultRange,
        maxDefaultRange);
  }

  private List<ProspectEvaluation> fromSpreadsheet(
      String idUser, SheetEvaluationJobRunner sheetProspectEvaluation) {
    var sheetProperties = sheetProspectEvaluation.getSheetProperties();
    var sheetRange = sheetProperties.getRanges();
    List<ProspectEvaluation> prospectEvals =
        prospectService.readEvaluationsFromSheetsWithoutFilter(
            idUser,
            sheetProspectEvaluation.getArtisanOwner(),
            sheetProperties.getSpreadsheetName(),
            sheetProperties.getSheetName(),
            sheetRange.getMin(),
            sheetRange.getMax());
    return prospectEvals;
  }
}
