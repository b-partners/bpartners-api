package app.bpartners.api.unit.service;

import static app.bpartners.api.endpoint.rest.model.JobStatusValue.*;
import static app.bpartners.api.integration.conf.utils.TestUtils.ACCOUNTHOLDER_ID;
import static app.bpartners.api.service.prospect.ProspectService.removeDuplications;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.SesConf;
import app.bpartners.api.model.mapper.ProspectMapper;
import app.bpartners.api.model.prospect.job.ProspectEvaluationJob;
import app.bpartners.api.repository.ProspectEvaluationJobRepository;
import app.bpartners.api.repository.ProspectRepository;
import app.bpartners.api.repository.expressif.ProspectEval;
import app.bpartners.api.repository.expressif.ProspectEvalInfo;
import app.bpartners.api.repository.expressif.ProspectResult;
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
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
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
  CustomerService customerService = mock(CustomerService.class);
  SheetApi sheetApi = mock(SheetApi.class);
  DriveApi driveApi = mock(DriveApi.class);
  ProspectMapper prospectMapper = mock(ProspectMapper.class);
  ProspectEvaluationJobRepository jobRepositoryMock = mock(ProspectEvaluationJobRepository.class);
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
          customerService,
          sheetApi,
          prospectMapper,
          jobRepositoryMock,
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
    var info = ProspectEvalInfo.builder()
            .name("name")
            .email("email")
            .phoneNumber("phoneNumber")
            .address("address")
            .build();
    var eval = ProspectEval.builder()
            .prospectEvalInfo(info)
            .build();
    var prospectResult = ProspectResult.builder()
            .prospectEval(eval)
            .build();

    var actual = removeDuplications(List.of(prospectResult));

    assertEquals(List.of(prospectResult), actual);
  }

  @Test
  void get_evaluation_jobs() {
    var idAccountHolder = "idAccountHolder";
    var statues = List.of(NOT_STARTED, IN_PROGRESS);
    var prospectEvaluationJob = ProspectEvaluationJob.builder().build();
    when(jobRepositoryMock.findAllByIdAccountHolderAndStatusesIn(idAccountHolder, statues))
            .thenReturn(List.of(prospectEvaluationJob));

    var actual = subject.getEvaluationJobs(idAccountHolder, statues);

    assertEquals(List.of(prospectEvaluationJob), actual);
  }
}
