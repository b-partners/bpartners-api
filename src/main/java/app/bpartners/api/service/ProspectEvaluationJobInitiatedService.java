package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.EventConf;
import app.bpartners.api.endpoint.event.model.gen.ProspectEvaluationJobInitiated;
import app.bpartners.api.endpoint.rest.mapper.ProspectRestMapper;
import app.bpartners.api.endpoint.rest.model.CalendarProvider;
import app.bpartners.api.endpoint.rest.model.EvaluatedProspect;
import app.bpartners.api.endpoint.rest.model.Geojson;
import app.bpartners.api.endpoint.rest.model.JobStatusValue;
import app.bpartners.api.endpoint.rest.model.NewInterventionOption;
import app.bpartners.api.endpoint.rest.model.ProspectStatus;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.CalendarEvent;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.model.prospect.ProspectStatusHistory;
import app.bpartners.api.model.prospect.job.EventJobRunner;
import app.bpartners.api.model.prospect.job.ProspectEvaluationJob;
import app.bpartners.api.model.prospect.job.SheetEvaluationJobRunner;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.ban.model.GeoPosition;
import app.bpartners.api.repository.expressif.ProspectEval;
import app.bpartners.api.repository.expressif.ProspectEvalInfo;
import app.bpartners.api.repository.expressif.ProspectResult;
import app.bpartners.api.repository.expressif.fact.NewIntervention;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.utils.TemplateResolverUtils;
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

import static app.bpartners.api.endpoint.rest.model.ContactNature.OLD_CUSTOMER;
import static app.bpartners.api.endpoint.rest.model.ContactNature.PROSPECT;
import static app.bpartners.api.endpoint.rest.model.JobStatusValue.FAILED;
import static app.bpartners.api.endpoint.rest.model.JobStatusValue.FINISHED;
import static app.bpartners.api.endpoint.rest.model.JobStatusValue.IN_PROGRESS;
import static app.bpartners.api.endpoint.rest.model.JobStatusValue.NOT_STARTED;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.service.utils.DateUtils.formatFrenchDatetime;
import static java.util.UUID.randomUUID;

@Service
@AllArgsConstructor
@Slf4j
public class ProspectEvaluationJobInitiatedService
    implements Consumer<ProspectEvaluationJobInitiated> {
  public static final String EVENT_EVALUATION_RESULT_EMAIL_TEMPLATE =
      "prospect_event_evaluation_result";
  public static final String SHEET_EVALUATION_RESULT_EMAIL_TEMPLATE =
      "prospect_sheet_evaluation_result";
  private final AccountHolderService holderService;
  private final ProspectService prospectService;
  private final CalendarService calendarService;
  private final BanApi banApi;
  private final SesService sesService;
  private final EventConf eventConf;
  private final ProspectRestMapper prospectRestMapper;

  @Override
  public void accept(ProspectEvaluationJobInitiated jobInitiated) {
    var job = jobInitiated.getJobRunner();
    var existingJob = prospectService.getEvaluationJob(job.getJobId());
    var runningJob = runningJob(existingJob);
    if (
        (runningJob != null && runningJob.getJobStatus().getValue() == IN_PROGRESS)) {
      var idUser = jobInitiated.getIdUser();
      var runningHolder =
          holderService.findDefaultByIdUser(jobInitiated.getIdUser());
      if (job.isEventConversionJob()) { //Only supported type for now
        try {
          var eventJobRunner = job.getEventJobRunner();
          var ranges = eventJobRunner.getEventDateRanges();
          var sheetProspectEvaluation = eventJobRunner.getSheetProspectEvaluation();
          var antiHarmRules = sheetProspectEvaluation.getEvaluationRules().getAntiHarmRules();
          var ratingProperties = sheetProspectEvaluation.getRatingProperties();
          List<CalendarEvent> calendarEvents = calendarService.getEvents(
              idUser,
              eventJobRunner.getCalendarId(),
              //TODO: check if local or google calendar is the most appropriate
              CalendarProvider.GOOGLE_CALENDAR,
              ranges.getFrom(),
              ranges.getTo());
          List<CalendarEvent> eventsWithAddress = calendarEvents.stream()
              .filter(event -> event.getLocation() != null)
              .collect(Collectors.toList());
          var locations = locationsFromEvents(eventsWithAddress);
          var prospectsByEvents = getProspectsToEvaluate(idUser, eventJobRunner, locations);
          for (CalendarEvent c : eventsWithAddress) {
            var prospects = prospectsByEvents.get(c.getLocation());
            var evaluatedProspects = prospectService.evaluateProspects(
                runningHolder.getId(),
                antiHarmRules,
                prospects,
                NewInterventionOption.ALL,
                ratingProperties.getMinProspectRating(),
                ratingProperties.getMinCustomerRating());
            String interventionDate = formatFrenchDatetime(c.getFrom().toInstant());
            String interventionLocation = c.getLocation();
            String emailSubject =
                String.format("Vos prospects à proximité de votre RDV du %s au %s",
                    interventionDate, interventionLocation);
            String emailBody =
                eventConversionEmailBody(
                    runningHolder,
                    evaluatedProspects,
                    interventionDate,
                    interventionLocation);
            terminateJob(runningJob, runningHolder, evaluatedProspects, emailSubject, emailBody);
          }
        } catch (Exception e) {
          updateJobStatus(runningJob, FAILED, e.getMessage());
        }
      } else if (job.isSpreadsheetEvaluationJob()) {
        try {
          var sheetJobRunner = job.getSheetJobRunner();
          AccountHolder holderOwner =
              holderService.getById(sheetJobRunner.getArtisanOwner());
          var evaluationRules = sheetJobRunner.getEvaluationRules();
          var antiHarmRules = evaluationRules.getAntiHarmRules();
          var ratingProperties = sheetJobRunner.getRatingProperties();
          var sheetProperties = sheetJobRunner.getSheetProperties();
          var evaluatedProspects = prospectService.evaluateProspects(
              holderOwner.getId(),
              antiHarmRules,
              fromSpreadsheet(idUser, sheetJobRunner),
              evaluationRules.getInterventionOption(),
              ratingProperties.getMinProspectRating(),
              ratingProperties.getMinCustomerRating());
          String emailSubject = "Les prospects évalués retenus pour " + holderOwner.getName()
              + " contenu dans la feuille " + sheetProperties.getSheetName();
          String emailBody = spreadsheetEvaluationEmailBody(holderOwner, evaluatedProspects);
          terminateJob(runningJob, runningHolder, evaluatedProspects, emailSubject, emailBody);
        } catch (Exception e) {
          updateJobStatus(runningJob, FAILED, e.getMessage());
        }
      } else {
        String exceptionMsg =
            "Only prospect evaluation job types"
                + " CALENDAR_EVENT_CONVERSION and SPREADSHEET_EVALUATION are supported for now";
        updateJobStatus(runningJob, FAILED, exceptionMsg);
        throw new NotImplementedException(exceptionMsg);
      }
    }
  }

  private void terminateJob(ProspectEvaluationJob runningJob,
                            AccountHolder accountHolder,
                            List<ProspectResult> evaluatedProspects,
                            String emailSubject,
                            String emailBody) {
    long durationMinutes = runningJob.getDuration()
        .toMinutes();
    long durationSeconds = runningJob.getDuration()
        .minusMinutes(durationMinutes)
        .toSeconds();
    List<Prospect> results = convertProspectFromResults(
        runningJob, accountHolder, evaluatedProspects);
    var finishedJob = updateJobStatus(runningJob.toBuilder()
            .results(results)
            .build(), FINISHED,
        getJobMessage(evaluatedProspects, durationMinutes, durationSeconds));
    if (finishedJob.getJobStatus().getValue() == FINISHED) {
      sendJobResultThroughEmail(accountHolder, finishedJob, emailSubject, emailBody);
    }
  }

  private String eventConversionEmailBody(AccountHolder accountHolder,
                                          List<ProspectResult> prospectResults,
                                          String interventionDate,
                                          String interventionLocation) {
    List<ProspectResult> dispatchedResults = prospectResults.stream()
        .filter(evalutedProspect ->
            evalutedProspect.getInterventionResult().getAddress()
                .equals(interventionLocation))
        .collect(Collectors.toList());
    List<EvaluatedProspect> evaluatedProspects = dispatchedResults.stream()
        .map(prospectRestMapper::toRest)
        .collect(Collectors.toList());
    List<EvaluatedProspect> oldCustomers = evaluatedProspects.stream()
        .filter(prospect -> prospect.getContactNature() != null
            && prospect.getContactNature() == OLD_CUSTOMER)
        .collect(Collectors.toList());
    List<EvaluatedProspect> newProspects = evaluatedProspects.stream()
        .filter(prospect -> prospect.getContactNature() != null
            && prospect.getContactNature() == PROSPECT)
        .collect(Collectors.toList());

    Context context = new Context();
    context.setVariable("evaluatedProspects", evaluatedProspects);
    context.setVariable("accountHolder", accountHolder);
    context.setVariable("oldCustomers", oldCustomers);
    context.setVariable("newProspects", newProspects);
    context.setVariable("interventionDate", interventionDate);
    context.setVariable("interventionLocation", interventionLocation);
    return TemplateResolverUtils.parseTemplateResolver(
        EVENT_EVALUATION_RESULT_EMAIL_TEMPLATE, context);
  }

  private String spreadsheetEvaluationEmailBody(AccountHolder accountHolder,
                                                List<ProspectResult> prospectResults) {
    List<EvaluatedProspect> evaluatedProspects = prospectResults.stream()
        .map(prospectRestMapper::toRest)
        .collect(Collectors.toList());
    List<EvaluatedProspect> oldCustomers = evaluatedProspects.stream()
        .filter(prospect -> prospect.getContactNature() != null
            && prospect.getContactNature() == OLD_CUSTOMER)
        .collect(Collectors.toList());
    List<EvaluatedProspect> newProspects = evaluatedProspects.stream()
        .filter(prospect -> prospect.getContactNature() != null
            && prospect.getContactNature() == PROSPECT)
        .collect(Collectors.toList());

    Context context = new Context();
    context.setVariable("evaluatedProspects", evaluatedProspects);
    context.setVariable("accountHolder", accountHolder);
    context.setVariable("oldCustomers", oldCustomers);
    context.setVariable("newProspects", newProspects);
    return TemplateResolverUtils.parseTemplateResolver(
        SHEET_EVALUATION_RESULT_EMAIL_TEMPLATE, context);
  }

  private void sendJobResultThroughEmail(AccountHolder accountHolder,
                                         ProspectEvaluationJob finishedJob,
                                         String emailSubject, String htmlBody) {
    try {
      sesService.sendEmail(
          accountHolder.getEmail(),
          null,
          //eventConf.getAdminEmail(), //TODO: confirm to put BPartners as CC
          emailSubject,
          htmlBody,
          List.of());
      log.info("Job(id=" + finishedJob.getId() + ") "
          + finishedJob.getJobStatus().getMessage());
    } catch (IOException | MessagingException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  private static List<Prospect> convertProspectFromResults(ProspectEvaluationJob runningJob,
                                                           AccountHolder accountHolder,
                                                           List<ProspectResult> evaluatedProspects) {
    return evaluatedProspects.stream()
        .map(result -> {
          ProspectEvalInfo info =
              result.getProspectEval().getProspectEvalInfo();
          var interventionResult =
              result.getInterventionResult();
          var customerResult = result.getCustomerInterventionResult();
          var ratingBuilder =
              Prospect.ProspectRating.builder().lastEvaluationDate(Instant.now());
          if (interventionResult != null && customerResult == null) {
            ratingBuilder.value(interventionResult.getRating());
          } else if (interventionResult == null && customerResult != null) {
            ratingBuilder.value(customerResult.getRating());
          }
          return Prospect.builder()
              .id(String.valueOf(randomUUID()))
              .idJob(runningJob.getId())
              .idHolderOwner(accountHolder.getId())
              .name(info.getName())
              .managerName(info.getManagerName())
              .email(info.getEmail())
              .phone(info.getPhoneNumber())
              .location(new Geojson()
                  .longitude(info.getCoordinates().getLongitude())
                  .latitude(info.getCoordinates().getLatitude())
              )
              .rating(ratingBuilder.build())
              .address(info.getAddress())
              .statusHistories(List.of(ProspectStatusHistory.builder()
                  .status(ProspectStatus.TO_CONTACT)
                  .updatedAt(Instant.now())
                  .build()))
              .townCode(Integer.valueOf(info.getPostalCode()))
              .defaultComment(info.getDefaultComment())
              .comment(null)
              .contractAmount(null)
              .prospectFeedback(null)
              .build();
        })
        .collect(Collectors.toList());
  }

  private static String getJobMessage(List<ProspectResult> evaluatedProspects, long durationMinutes,
                                      long durationSeconds) {
    return "Successfully processed after "
        + durationMinutes + " minutes "
        + durationSeconds + " seconds with " + evaluatedProspects.size()
        + " evaluated prospects or old customers";
  }

  private ProspectEvaluationJob updateJobStatus(ProspectEvaluationJob job,
                                                JobStatusValue jobStatusValue,
                                                String jobMessage) {
    ProspectEvaluationJob clonedJob = job.toBuilder()
        .jobStatus(job.getJobStatus()
            .value(jobStatusValue)
            .message(jobMessage))
        .endedAt(jobStatusValue == FINISHED || jobStatusValue == FAILED ? Instant.now()
            : null)
        .build();
    return prospectService.saveEvaluationJobs(List.of(clonedJob)).get(0);
  }

  private ProspectEvaluationJob runningJob(ProspectEvaluationJob existingJob) {
    if (existingJob.getJobStatus().getValue() == NOT_STARTED) {
      return updateJobStatus(existingJob, IN_PROGRESS, null);
    }
    return null;
  }

  private HashMap<String, List<ProspectEval>> getProspectsToEvaluate(String idUser,
                                                                     EventJobRunner eventJobRunner,
                                                                     List<String> locations) {
    HashMap<String, List<ProspectEval>> prospectsByEvents = new HashMap<>();
    var newProspects = fromSpreadsheet(idUser, eventJobRunner);
    var evaluationRules = eventJobRunner.getSheetProspectEvaluation().getEvaluationRules();
    var antiHarmRules = evaluationRules.getAntiHarmRules();
    locations.forEach(calendarEventLocation -> {
      GeoPosition eventAddressPos = banApi.fSearch(calendarEventLocation);
      List<ProspectEval> prospectsToEvaluate = new ArrayList<>();
      newProspects.forEach(prospect -> {
        NewIntervention clonedRule = (NewIntervention) prospect.getDepaRule();
        prospectsToEvaluate.add(
            prospect.toBuilder()
                .prospectOwnerId(eventJobRunner.getSheetProspectEvaluation().getArtisanOwner())
                .id(String.valueOf(randomUUID()))
                .ratRemoval(antiHarmRules.isRatRemoval())
                .disinfection(antiHarmRules.isDisinfection())
                .insectControl(antiHarmRules.isInsectControl())
                .depaRule(clonedRule.toBuilder()
                    .newIntAddress(calendarEventLocation)
                    .coordinate(eventAddressPos.getCoordinates())
                    .distNewIntAndProspect(eventAddressPos.getCoordinates()
                        .getDistanceFrom(prospect.getProspectEvalInfo().getCoordinates()))
                    .build())
                .build());
      });
      prospectsByEvents.put(calendarEventLocation, prospectsToEvaluate);
    });
    return prospectsByEvents;
  }

  private List<String> locationsFromEvents(List<CalendarEvent> calendarEvents) {
    return calendarEvents.stream()
        .map(CalendarEvent::getLocation)
        .collect(Collectors.toList());
  }

  private List<ProspectEval> fromSpreadsheet(String idUser, EventJobRunner eventJobRunner) {
    return fromSpreadsheet(idUser, eventJobRunner.getSheetProspectEvaluation());
  }

  private List<ProspectEval> fromSpreadsheet(String idUser,
                                             SheetEvaluationJobRunner sheetProspectEvaluation) {
    var sheetProperties = sheetProspectEvaluation.getSheetProperties();
    var sheetRange = sheetProperties.getRanges();
    List<ProspectEval> prospectEvals = prospectService.readEvaluationsFromSheets(
        idUser,
        sheetProspectEvaluation.getArtisanOwner(),
        sheetProperties.getSpreadsheetName(),
        sheetProperties.getSheetName(),
        sheetRange.getMin(),
        sheetRange.getMax());
    return prospectEvals;
  }
}
