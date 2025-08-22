package app.bpartners.api.unit.service;

import static app.bpartners.api.endpoint.rest.model.InterventionType.INSECT_CONTROL;
import static app.bpartners.api.endpoint.rest.model.JobStatusValue.*;
import static app.bpartners.api.endpoint.rest.model.NewInterventionOption.ALL;
import static app.bpartners.api.integration.conf.utils.TestUtils.ACCOUNTHOLDER_ID;
import static app.bpartners.api.service.prospect.ProspectService.removeDuplications;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.SesConf;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Location;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.mapper.ProspectMapper;
import app.bpartners.api.model.prospect.job.AntiHarmRules;
import app.bpartners.api.model.prospect.job.EventJobRunner;
import app.bpartners.api.model.prospect.job.ProspectEvaluationJob;
import app.bpartners.api.model.prospect.job.ProspectEvaluationJobRunner;
import app.bpartners.api.repository.ProspectEvaluationJobRepository;
import app.bpartners.api.repository.ProspectRepository;
import app.bpartners.api.repository.expressif.ProspectEval;
import app.bpartners.api.repository.expressif.ProspectEvalInfo;
import app.bpartners.api.repository.expressif.ProspectResult;
import app.bpartners.api.repository.expressif.fact.NewIntervention;
import app.bpartners.api.repository.google.calendar.CalendarApi;
import app.bpartners.api.repository.google.calendar.drive.DriveApi;
import app.bpartners.api.repository.google.sheets.SheetApi;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import app.bpartners.api.service.SnsService;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.customer.CustomerService;
import app.bpartners.api.service.dataprocesser.ProspectDataProcesser;
import app.bpartners.api.service.event.ProspectUpdatedService;
import app.bpartners.api.service.prospect.ProspectService;
import app.bpartners.api.service.prospect.ProspectStatusService;
import app.bpartners.api.service.user.UserService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import app.bpartners.api.service.utils.GeoUtils;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProspectServiceTest {
  ProspectRepository prospectRepositoryMock = mock(ProspectRepository.class);
  ProspectDataProcesser dataProcesserMock = mock(ProspectDataProcesser.class);
  AccountHolderJpaRepository accountHolderJpaRepositoryMock =
      mock(AccountHolderJpaRepository.class);
  SesService sesServiceMock = mock(SesService.class);
  CustomerService customerServiceMock = mock(CustomerService.class);
  SheetApi sheetApi = mock(SheetApi.class);
  DriveApi driveApi = mock(DriveApi.class);
  ProspectMapper prospectMapper = mock(ProspectMapper.class);
  ProspectEvaluationJobRepository evalJobRepositoryMock =
      mock(ProspectEvaluationJobRepository.class);
  EventProducer eventProducerMock = mock(EventProducer.class);
  SesConf sesConfMock = mock(SesConf.class);
  ProspectStatusService prospectStatusService = mock(ProspectStatusService.class);
  UserService userServiceMock = mock(UserService.class);
  SnsService snsServiceMock = mock(SnsService.class);
  ProspectUpdatedService prospectUpdatedService = mock(ProspectUpdatedService.class);
  CalendarApi calendarApiMock = mock(CalendarApi.class);
  ProspectService subject =
      new ProspectService(
          prospectRepositoryMock,
          dataProcesserMock,
          accountHolderJpaRepositoryMock,
          sesServiceMock,
          customerServiceMock,
          sheetApi,
          prospectMapper,
          evalJobRepositoryMock,
          eventProducerMock,
          sesConfMock,
          prospectStatusService,
          snsServiceMock,
          userServiceMock,
          calendarApiMock,
          mock(),
          new CustomDateFormatter());

  @BeforeEach
  void setup() {
    when(accountHolderJpaRepositoryMock.findAll())
        .thenReturn(
            List.of(
                HAccountHolder.builder().id(ACCOUNTHOLDER_ID).build(),
                HAccountHolder.builder().id("fake_accountholder_id").build()));
    when(prospectRepositoryMock.needsProspects(ACCOUNTHOLDER_ID, LocalDate.now()))
        .thenAnswer(i -> Objects.equals(i.getArgument(0), ACCOUNTHOLDER_ID));
    when(prospectRepositoryMock.isSogefiProspector(any()))
        .thenAnswer(i -> Objects.equals(i.getArgument(0), ACCOUNTHOLDER_ID));
  }

  @Test
  void should_send_email() throws MessagingException, IOException {
    subject.prospect();

    verify(sesServiceMock, times(1)).sendEmail(any(), any(), any(), any(), any());
  }

  @Test
  void should_not_send_email() throws MessagingException, IOException {
    when(prospectRepositoryMock.needsProspects(ACCOUNTHOLDER_ID, LocalDate.now()))
        .thenReturn(false);
    when(prospectRepositoryMock.isSogefiProspector(any())).thenReturn(false);

    subject.prospect();

    verify(sesServiceMock, never()).sendEmail(any(), any(), any(), any(), any());
  }

  @Test
  void remove_duplications() {
    var info =
        ProspectEvalInfo.builder()
            .name("name")
            .email("email")
            .phoneNumber("phoneNumber")
            .address("address")
            .build();
    var eval = ProspectEval.builder().prospectEvalInfo(info).build();
    var prospectResult = ProspectResult.builder().prospectEval(eval).build();

    var actual = removeDuplications(List.of(prospectResult));

    assertEquals(List.of(prospectResult), actual);
  }

  @Test
  void get_evaluation_jobs() {
    var idAccountHolder = "idAccountHolder";
    var statues = List.of(NOT_STARTED, IN_PROGRESS);
    var prospectEvaluationJob = ProspectEvaluationJob.builder().build();
    when(evalJobRepositoryMock.findAllByIdAccountHolderAndStatusesIn(idAccountHolder, statues))
        .thenReturn(List.of(prospectEvaluationJob));

    var actual = subject.getEvaluationJobs(idAccountHolder, statues);

    assertEquals(List.of(prospectEvaluationJob), actual);
  }

  @Test
  void run_evaluation_jobs_bad_request() {
    var userId = "userId";
    var ahId = "ahId";
    var eventJobRunner = EventJobRunner.builder().build();
    var anyEventConversionJob =
        ProspectEvaluationJobRunner.builder().eventJobRunner(eventJobRunner).build();
    when(calendarApiMock.hasValidToken(any())).thenReturn(false);

    assertThrows(
        BadRequestException.class,
        () -> subject.runEvaluationJobs(userId, ahId, List.of(anyEventConversionJob)));
  }

  @Test
  void run_evaluation_jobs_ok() {
    var userId = "userId";
    var ahId = "ahId";
    var metadata = mock(Map.class);
    var eventJobRunner = EventJobRunner.builder().build();
    var jobRunner =
        ProspectEvaluationJobRunner.builder()
            .jobId("jobId")
            .metadata(metadata)
            .eventJobRunner(eventJobRunner)
            .build();
    when(calendarApiMock.hasValidToken(any())).thenReturn(true);
    var savedJobs = ProspectEvaluationJob.builder().build();
    when(evalJobRepositoryMock.saveAll(anyList())).thenReturn(List.of(savedJobs));
    doNothing().when(eventProducerMock).accept(anyList());

    var actual = subject.runEvaluationJobs(userId, ahId, List.of(jobRunner));

    assertEquals(List.of(savedJobs), actual);
  }

  @Test
  void save_evaluation_jobs() {
    var evaluationJobs = ProspectEvaluationJob.builder().build();
    when(evalJobRepositoryMock.saveAll(anyList())).thenReturn(List.of(evaluationJobs));

    var actual = subject.saveEvaluationJobs(List.of(evaluationJobs));

    assertEquals(List.of(evaluationJobs), actual);
  }

  @Test
  void evaluate_prospect() {
    var ahId = "ahId";
    var antiHarmRules = AntiHarmRules.builder().interventionTypes(List.of(INSECT_CONTROL)).build();
    var coordinate = GeoUtils.Coordinate.builder().latitude(100.0).longitude(200.0).build();
    var oldCustomer = NewIntervention.OldCustomer.builder().build();
    var depaRule =
        NewIntervention.builder()
            .oldCustomer(oldCustomer)
            .coordinate(coordinate)
            .interventionType("INSECT_CONTROL")
            .build();
    var info = ProspectEvalInfo.builder().build();
    var prospectsToEvaluate =
        ProspectEval.builder()
            .depaRule(depaRule)
            .prospectOwnerId(ahId)
            .prospectEvalInfo(info)
            .build();
    var minProspectRating = 0.1;
    var minCustomerRating = 0.1;
    var zipCode = 1;
    var location = Location.builder().coordinate(coordinate).build();
    var customer =
        Customer.builder()
            .id("customerId")
            .firstName("firstName")
            .lastName("lastName")
            .email("email")
            .phone("phone")
            .city("city")
            .zipCode(zipCode)
            .address("address")
            .location(location)
            .build();
    when(customerServiceMock.findByAccountHolderId(any())).thenReturn(List.of(customer));
    var interventionResult = new ProspectResult.InterventionResult(1.1, 1.1, "address");
    var customerInterventionResult =
        new ProspectResult.CustomerInterventionResult(
            1.1, 1.1, "address", "idCustomerInterventionResult");
    var prospectResult =
        ProspectResult.builder()
            .prospectEval(prospectsToEvaluate)
            .interventionResult(interventionResult)
            .customerInterventionResult(customerInterventionResult)
            .build();
    when(prospectRepositoryMock.evaluate(anyList())).thenReturn(List.of(prospectResult));

    var actual =
        subject.evaluateProspects(
            ahId,
            antiHarmRules,
            List.of(prospectsToEvaluate),
            ALL,
            minProspectRating,
            minCustomerRating);

    assertEquals(List.of(prospectResult), actual);
    verify(customerServiceMock, times(1)).findByAccountHolderId(any());
  }
}
