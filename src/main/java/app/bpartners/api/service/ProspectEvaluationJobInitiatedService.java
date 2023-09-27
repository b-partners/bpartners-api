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
import app.bpartners.api.model.prospect.job.EventJobRunner;
import app.bpartners.api.model.prospect.job.ProspectEvaluationJob;
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
  public static final String PROSPECT_EVALUATION_RESULT_EMAIL_TEMPLATE =
      "prospect_evaluation_result";
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
              CalendarProvider.LOCAL,
              ranges.getFrom(),
              ranges.getTo());
          List<CalendarEvent> eventsWithAddress = calendarEvents.stream()
              .filter(event -> event.getLocation() != null)
              .collect(Collectors.toList());
          var locations = locationsFromEvents(eventsWithAddress);
          var prospectsByEvents = getProspectsToEvaluate(idUser, eventJobRunner, locations);
          var accountHolder =
              holderService.findDefaultByIdUser(jobInitiated.getIdUser());
          for (CalendarEvent c : eventsWithAddress) {
            var prospects = prospectsByEvents.get(c.getLocation());
            var evaluatedProspects = prospectService.evaluateProspects(
                accountHolder.getId(),
                antiHarmRules,
                prospects,
                NewInterventionOption.ALL,
                ratingProperties.getMinProspectRating(),
                ratingProperties.getMinCustomerRating());
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
              //TODO: associate evaluated prospect to finished job
              String interventionDate = formatFrenchDatetime(c.getFrom().toInstant());
              String interventionLocation = c.getLocation();
              String emailSubject =
                  String.format("Vos prospects à proximité de votre RDV du %s au %s",
                      interventionDate, interventionLocation);
              var dispatchedProspects = evaluatedProspects.stream()
                  .filter(evalutedProspect ->
                      evalutedProspect.getInterventionResult().getAddress()
                          .equals(interventionLocation))
                  .collect(Collectors.toList());
              String htmlBody = emailHtmlBody(
                  accountHolder,
                  dispatchedProspects,
                  interventionDate,
                  interventionLocation);
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
          }
        } catch (Exception e) {
          updateJobStatus(runningJob, FAILED, e.getMessage());
        }
      } else {
        String exceptionMsg =
            "Only prospect evaluation job type [CALENDAR_EVENT_CONVERSION] is supported for now";
        updateJobStatus(runningJob, FAILED, exceptionMsg);
        throw new NotImplementedException(exceptionMsg);
      }
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
              .email(info.getEmail())
              .phone(info.getPhoneNumber())
              .location(new Geojson()
                  .longitude(info.getCoordinates().getLongitude())
                  .latitude(info.getCoordinates().getLatitude())
              )
              .rating(ratingBuilder.build())
              .address(info.getAddress())
              .status(ProspectStatus.TO_CONTACT)
              .townCode(Integer.valueOf(info.getPostalCode()))
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
    var sheetProspectEvaluation =
        eventJobRunner.getSheetProspectEvaluation();
    var sheetProperties = sheetProspectEvaluation.getSheetProperties();
    var sheetRange = sheetProperties.getRanges();
    return prospectService.readEvaluationsFromSheets(
        idUser,
        sheetProspectEvaluation.getArtisanOwner(),
        sheetProperties.getSpreadsheetName(),
        sheetProperties.getSheetName(),
        sheetRange.getMin(),
        sheetRange.getMax());
  }

  public String emailHtmlBody(AccountHolder accountHolder,
                              List<ProspectResult> prospectResults,
                              String interventionDate,
                              String interventionLocation) {
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
    context.setVariable("interventionDate", interventionDate);
    context.setVariable("interventionLocation", interventionLocation);
    return TemplateResolverUtils.parseTemplateResolver(
        PROSPECT_EVALUATION_RESULT_EMAIL_TEMPLATE, context);
  }
}
