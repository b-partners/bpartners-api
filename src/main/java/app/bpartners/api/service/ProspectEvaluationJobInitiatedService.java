package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.EventConf;
import app.bpartners.api.endpoint.event.model.gen.ProspectEvaluationJobInitiated;
import app.bpartners.api.endpoint.rest.mapper.ProspectRestMapper;
import app.bpartners.api.endpoint.rest.model.EvaluatedProspect;
import app.bpartners.api.endpoint.rest.model.JobStatusValue;
import app.bpartners.api.endpoint.rest.model.NewInterventionOption;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.CalendarEvent;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.prospect.job.EventJobRunner;
import app.bpartners.api.model.prospect.job.ProspectEvaluationJob;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.ban.model.GeoPosition;
import app.bpartners.api.repository.expressif.ProspectEval;
import app.bpartners.api.repository.expressif.ProspectResult;
import app.bpartners.api.repository.expressif.fact.NewIntervention;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.utils.TemplateResolverUtils;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.mail.MessagingException;
import lombok.AllArgsConstructor;
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
      /*TODO: if events is received again.
       * In particular, if we want to handle this processing through separate lambda container for perf
       * existingJob.getJobStatus().getValue() == IN_PROGRESS ||*/
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
              ranges.getFrom(),
              ranges.getTo());
          List<CalendarEvent> eventsWithAddress = calendarEvents.stream()
              .filter(event -> event.getLocation() != null)
              .collect(Collectors.toList());
          var locations = locationsFromEvents(eventsWithAddress);
          var prospects = getProspectsToEvaluate(idUser, eventJobRunner, locations);
          var accountHolder =
              holderService.findDefaultByIdUser(jobInitiated.getIdUser());
          var evaluatedProspects = prospectService.evaluateProspects(
              accountHolder.getId(),
              antiHarmRules,
              prospects,
              NewInterventionOption.ALL,
              ratingProperties.getMinProspectRating(),
              ratingProperties.getMinCustomerRating());
          long durationMinutes = runningJob.getDuration().toMinutes();
          long durationSeconds = runningJob.getDuration().minusMinutes(durationMinutes)
              .toSeconds();
          var finishedJob = updateJobStatus(runningJob, FINISHED,
              "Job successfully processed after "
                  + durationMinutes + " minutes "
                  + durationSeconds + " seconds with " + evaluatedProspects.size()
                  + " evaluated prospects or old customers");
          if (finishedJob.getJobStatus().getValue() == FINISHED) {
            //TODO: associate evaluated prospect to finished job
            eventsWithAddress.forEach(event -> {
              String interventionDate = formatFrenchDatetime(event.getFrom().toInstant());
              String interventionLocation = event.getLocation();
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
              } catch (IOException | MessagingException e) {
                throw new ApiException(SERVER_EXCEPTION, e);
              }
            });
          }
        } catch (Exception e) {
          updateJobStatus(runningJob, FAILED, e.getMessage());
        }
      } else {
        throw new NotImplementedException(
            "Only prospect evaluation job type [CALENDAR_EVENT_CONVERSION] is supported for now");
      }
    }
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

  private List<ProspectEval> getProspectsToEvaluate(String idUser,
                                                    EventJobRunner eventJobRunner,
                                                    List<String> locations) {
    List<ProspectEval> prospectsToEvaluate = new ArrayList<>();
    var newProspects = fromSpreadsheet(idUser, eventJobRunner);
    var evaluationRules = eventJobRunner.getSheetProspectEvaluation().getEvaluationRules();
    var antiHarmRules = evaluationRules.getAntiHarmRules();
    locations.forEach(calendarEventLocation -> {
      GeoPosition eventAddressPos = banApi.fSearch(calendarEventLocation);
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
    });
    return prospectsToEvaluate;
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
