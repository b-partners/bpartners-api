package app.bpartners.api.integration.event;

import static app.bpartners.api.endpoint.rest.model.ContactNature.OLD_CUSTOMER;
import static app.bpartners.api.endpoint.rest.model.InterventionType.DISINFECTION;
import static app.bpartners.api.endpoint.rest.model.JobStatusValue.IN_PROGRESS;
import static app.bpartners.api.endpoint.rest.model.JobStatusValue.NOT_STARTED;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.SesConf;
import app.bpartners.api.endpoint.event.model.ProspectEvaluationJobInitiated;
import app.bpartners.api.endpoint.rest.mapper.ProspectRestMapper;
import app.bpartners.api.endpoint.rest.model.EvaluatedProspect;
import app.bpartners.api.endpoint.rest.model.ProspectEvaluationJobStatus;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.CalendarEvent;
import app.bpartners.api.model.User;
import app.bpartners.api.model.prospect.job.*;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.ban.model.GeoPosition;
import app.bpartners.api.repository.expressif.ProspectEval;
import app.bpartners.api.repository.expressif.ProspectEvalInfo;
import app.bpartners.api.repository.expressif.ProspectResult;
import app.bpartners.api.repository.expressif.fact.NewIntervention;
import app.bpartners.api.service.SnsService;
import app.bpartners.api.service.accountholder.AccountHolderService;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.event.ProspectEvaluationJobInitiatedService;
import app.bpartners.api.service.google.calendar.CalendarService;
import app.bpartners.api.service.prospect.ProspectService;
import app.bpartners.api.service.user.UserService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import app.bpartners.api.service.utils.GeoUtils;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProspectEvaluationJobInitiatedServiceTest {
  AccountHolderService holderServiceMock = mock();
  ProspectService prospectServiceMock = mock();
  CalendarService calendarServiceMock = mock();
  BanApi banApiMock = mock();
  SesService sesServiceMock = mock();
  SesConf sesConfMock = mock();
  ProspectRestMapper prospectRestMapperMock = mock();
  UserService userServiceMock = mock();
  SnsService snsServiceMock = mock();
  TemplateResolverEngine templateResolverEngine = mock();
  CustomDateFormatter customDateFormatter = mock();
  ProspectEvaluationJobInitiatedService subject =
      new ProspectEvaluationJobInitiatedService(
          holderServiceMock,
          prospectServiceMock,
          calendarServiceMock,
          banApiMock,
          sesServiceMock,
          sesConfMock,
          prospectRestMapperMock,
          userServiceMock,
          snsServiceMock,
          templateResolverEngine,
          customDateFormatter);

  @Test
  void accept() {
    var idUser = "idUser";
    var antiHarmRules = AntiHarmRules.builder().interventionTypes(List.of(DISINFECTION)).build();
    var evaluationRules = EvaluationRules.builder().antiHarmRules(antiHarmRules).build();
    var ratingPropreties = RatingProperties.builder().build();
    var eventJobRunner =
        EventJobRunner.builder()
            .eventDateRanges(EventJobRunner.EventDateRanges.builder().build())
            .evaluationRules(evaluationRules)
            .ratingProperties(ratingPropreties)
            .artisanOwner("artisanOwner")
            .build();
    var job =
        ProspectEvaluationJobRunner.builder().jobId("jobId").eventJobRunner(eventJobRunner).build();
    var jobInitiated =
        ProspectEvaluationJobInitiated.builder().idUser(idUser).jobRunner(job).build();
    var jobStatusNotStarted = mock(ProspectEvaluationJobStatus.class);
    when(jobStatusNotStarted.getValue()).thenReturn(NOT_STARTED);
    when(jobStatusNotStarted.value(any())).thenReturn(jobStatusNotStarted);
    when(jobStatusNotStarted.message(anyString())).thenReturn(jobStatusNotStarted);
    var existingJob = ProspectEvaluationJob.builder().jobStatus(jobStatusNotStarted).build();
    when(prospectServiceMock.getEvaluationJob(anyString())).thenReturn(existingJob);
    var jobStatusInProgress = mock(ProspectEvaluationJobStatus.class);
    when(jobStatusInProgress.getValue()).thenReturn(IN_PROGRESS);
    when(jobStatusInProgress.value(any())).thenReturn(jobStatusInProgress);
    when(jobStatusInProgress.message(anyString())).thenReturn(jobStatusInProgress);
    var runningJob = ProspectEvaluationJob.builder().jobStatus(jobStatusInProgress).build();
    when(prospectServiceMock.saveEvaluationJobs(anyList())).thenReturn(List.of(runningJob));
    var accountHolder = AccountHolder.builder().name("name").build();
    var user = User.builder().accountHolders(List.of(accountHolder)).build();
    when(userServiceMock.getUserById(anyString())).thenReturn(user);
    var runningHolder = AccountHolder.builder().build();
    when(holderServiceMock.findDefaultByIdUser(anyString())).thenReturn(runningHolder);
    var eventsWithAddress = CalendarEvent.builder().location("location").build();
    when(calendarServiceMock.getEvents(anyString(), anyString(), any(), any(), any()))
        .thenReturn(List.of(eventsWithAddress));
    var clonedRule = NewIntervention.builder().build();
    var newProspects =
        ProspectEval.builder()
            .prospectEvalInfo(
                ProspectEvalInfo.builder()
                    .coordinates(GeoUtils.Coordinate.builder().build())
                    .build())
            .depaRule(clonedRule)
            .build();
    when(prospectServiceMock.readEvaluationsFromSheetsWithoutFilter(
            anyString(), any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(List.of(newProspects));
    var eventAddressCoordinates = GeoUtils.Coordinate.builder().build();
    var eventAddressPos = GeoPosition.builder().coordinates(eventAddressCoordinates).build();
    when(banApiMock.fSearch(anyString())).thenReturn(eventAddressPos);
    var interventionResult = new ProspectResult.InterventionResult(1.0, 1.0, "location");
    var evaluatedProspects =
        ProspectResult.builder().interventionResult(interventionResult).build();
    when(prospectServiceMock.evaluateProspects(
            any(), any(), anyList(), any(), anyDouble(), anyDouble()))
        .thenReturn(List.of(evaluatedProspects));
    when(customDateFormatter.formatFrenchDate(any(LocalDate.class))).thenReturn("19/08/2025 10:57");
    var evaluatedProspectsRest = mock(EvaluatedProspect.class);
    when(evaluatedProspectsRest.getContactNature()).thenReturn(OLD_CUSTOMER);
    when(prospectRestMapperMock.toRest(any(ProspectResult.class)))
        .thenReturn(evaluatedProspectsRest);
    when(templateResolverEngine.parseTemplateResolver(anyString(), any())).thenReturn("emailBody");

    subject.accept(jobInitiated);

    verify(prospectServiceMock).getEvaluationJob(anyString());
    verify(prospectServiceMock).saveEvaluationJobs(anyList());
    verify(userServiceMock).getUserById(anyString());
    verify(holderServiceMock).findDefaultByIdUser(anyString());
    verify(calendarServiceMock).getEvents(any(), any(), any(), any(), any());
    verify(prospectServiceMock)
        .readEvaluationsFromSheetsWithoutFilter(any(), any(), any(), any(), anyInt(), anyInt());
  }
}
