package app.bpartners.api.integration.event;

import static app.bpartners.api.endpoint.rest.model.ContactNature.OLD_CUSTOMER;
import static app.bpartners.api.endpoint.rest.model.InterventionType.DISINFECTION;
import static app.bpartners.api.endpoint.rest.model.JobStatusValue.IN_PROGRESS;
import static app.bpartners.api.endpoint.rest.model.JobStatusValue.NOT_STARTED;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import app.bpartners.api.LogCaptor;
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
import java.time.Instant;
import java.time.ZonedDateTime;
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
  void accept_ok() {
    var idUser = "idUser";
    var antiHarmRules = AntiHarmRules.builder().interventionTypes(List.of(DISINFECTION)).build();
    var evaluationRules = EvaluationRules.builder().antiHarmRules(antiHarmRules).build();
    var ratingPropreties = RatingProperties.builder().build();
    var eventJobRunner =
        EventJobRunner.builder()
            .eventDateRanges(
                EventJobRunner.EventDateRanges.builder()
                    .from(Instant.now())
                    .to(Instant.now())
                    .build())
            .evaluationRules(evaluationRules)
            .ratingProperties(ratingPropreties)
            .artisanOwner("artisanOwner")
            .calendarId("calendarId")
            .build();
    var job =
        ProspectEvaluationJobRunner.builder().jobId("jobId").eventJobRunner(eventJobRunner).build();
    var jobInitiated =
        ProspectEvaluationJobInitiated.builder().idUser(idUser).jobRunner(job).build();
    var jobStatusNotStarted = mock(ProspectEvaluationJobStatus.class);
    when(jobStatusNotStarted.getValue()).thenReturn(NOT_STARTED);
    when(jobStatusNotStarted.value(any())).thenReturn(jobStatusNotStarted);
    when(jobStatusNotStarted.message(anyString())).thenReturn(jobStatusNotStarted);
    var existingJob =
        ProspectEvaluationJob.builder()
            .id("jobId")
            .idAccountHolder("holderId")
            .jobStatus(jobStatusNotStarted)
            .startedAt(Instant.now())
            .build();
    when(prospectServiceMock.getEvaluationJob(anyString())).thenReturn(existingJob);
    var jobStatusInProgress = mock(ProspectEvaluationJobStatus.class);
    when(jobStatusInProgress.getValue()).thenReturn(IN_PROGRESS);
    when(jobStatusInProgress.value(any())).thenReturn(jobStatusInProgress);
    when(jobStatusInProgress.message(anyString())).thenReturn(jobStatusInProgress);
    var runningJob =
        ProspectEvaluationJob.builder()
            .id("jobId")
            .idAccountHolder("holderId")
            .jobStatus(jobStatusInProgress)
            .startedAt(Instant.now())
            .build();
    when(prospectServiceMock.saveEvaluationJobs(anyList())).thenReturn(List.of(runningJob));
    var accountHolder = AccountHolder.builder().name("name").build();
    var bankConnectionId = 2L;
    var user =
        User.builder()
            .id(idUser)
            .bankConnectionId(bankConnectionId)
            .accountHolders(List.of(accountHolder))
            .build();
    when(userServiceMock.getUserById(anyString())).thenReturn(user);
    var runningHolder = AccountHolder.builder().build();
    when(holderServiceMock.findDefaultByIdUser(anyString())).thenReturn(runningHolder);
    var eventsWithAddress =
        CalendarEvent.builder().location("location").from(ZonedDateTime.now()).build();
    when(calendarServiceMock.getEvents(anyString(), anyString(), any(), any(), any()))
        .thenReturn(List.of(eventsWithAddress));
    var newProspect =
        ProspectEval.builder()
            .prospectEvalInfo(
                ProspectEvalInfo.builder()
                    .coordinates(GeoUtils.Coordinate.builder().latitude(1.0).longitude(2.0).build())
                    .postalCode("75001")
                    .name("Test")
                    .build())
            .depaRule(NewIntervention.builder().build())
            .build();
    when(prospectServiceMock.readEvaluationsFromSheetsWithoutFilter(
            anyString(), any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(List.of(newProspect));
    var eventAddressCoordinates =
        GeoUtils.Coordinate.builder().latitude(1.0).longitude(2.0).build();
    var eventAddressPos = GeoPosition.builder().coordinates(eventAddressCoordinates).build();
    when(banApiMock.fSearch(any())).thenReturn(eventAddressPos);
    var interventionResult = new ProspectResult.InterventionResult(1.0, 1.0, "location");
    var evaluatedProspects =
        ProspectResult.builder().interventionResult(interventionResult).build();
    when(prospectServiceMock.evaluateProspects(
            any(), any(), anyList(), any(), anyDouble(), anyDouble()))
        .thenReturn(List.of(evaluatedProspects));
    when(customDateFormatter.formatFrenchDatetime(any(Instant.class)))
        .thenReturn("19/08/2025 10:57");
    var evaluatedProspectsRest = mock(EvaluatedProspect.class);
    when(evaluatedProspectsRest.getContactNature()).thenReturn(OLD_CUSTOMER);
    when(prospectRestMapperMock.toRest(any(ProspectResult.class)))
        .thenReturn(evaluatedProspectsRest);
    when(templateResolverEngine.parseTemplateResolver(anyString(), any())).thenReturn("emailBody");

    subject.accept(jobInitiated);

    verify(prospectServiceMock).getEvaluationJob(anyString());
    verify(prospectServiceMock, times(2)).saveEvaluationJobs(anyList());
    verify(userServiceMock).getUserById(anyString());
    verify(holderServiceMock).findDefaultByIdUser(anyString());
    verify(calendarServiceMock).getEvents(any(), any(), any(), any(), any());
    verify(prospectServiceMock)
        .readEvaluationsFromSheetsWithoutFilter(any(), any(), any(), any(), anyInt(), anyInt());
    verify(banApiMock).fSearch(any());
    verify(prospectServiceMock)
        .readEvaluationsFromSheetsWithoutFilter(any(), any(), any(), any(), anyInt(), anyInt());
    verify(customDateFormatter, times(1)).formatFrenchDatetime(any());
    verify(templateResolverEngine, times(1)).parseTemplateResolver(anyString(), any());
  }

  @Test
  void accept_ko() {
    var logCaptor = new LogCaptor();
    logCaptor.configure(ProspectEvaluationJobInitiatedService.class);
    var ranges = EventJobRunner.EventDateRanges.builder().build();
    var antiHarmRules = AntiHarmRules.builder().build();
    var ratingProperties = RatingProperties.builder().build();
    var eventJobRunner =
        EventJobRunner.builder()
            .eventDateRanges(ranges)
            .evaluationRules(EvaluationRules.builder().antiHarmRules(antiHarmRules).build())
            .ratingProperties(ratingProperties)
            .build();
    var job =
        ProspectEvaluationJobRunner.builder().jobId("jobId").eventJobRunner(eventJobRunner).build();
    var jobInitiated =
        ProspectEvaluationJobInitiated.builder().jobRunner(job).idUser("idUser").build();
    var jobStatus = mock(ProspectEvaluationJobStatus.class);
    when(jobStatus.getValue()).thenReturn(NOT_STARTED);
    var runningJob =
        ProspectEvaluationJob.builder()
            .jobStatus(jobStatus)
            .id("jobId")
            .idAccountHolder("holderId")
            .startedAt(Instant.now())
            .build();
    when(prospectServiceMock.getEvaluationJob(anyString())).thenReturn(runningJob);
    when(prospectServiceMock.saveEvaluationJobs(anyList())).thenReturn(List.of(runningJob));
    var user = User.builder().build();
    when(userServiceMock.getUserById(anyString())).thenReturn(user);
    var runningHolder = AccountHolder.builder().build();
    when(holderServiceMock.findDefaultByIdUser(anyString())).thenReturn(runningHolder);
    when(calendarServiceMock.getEvents(any(), any(), any(), any(), any()))
        .thenThrow(new RuntimeException());
    when(sesConfMock.getAdminEmail()).thenReturn("admin@admin.com");

    assertThrows(RuntimeException.class, () -> subject.accept(jobInitiated));
  }
}
