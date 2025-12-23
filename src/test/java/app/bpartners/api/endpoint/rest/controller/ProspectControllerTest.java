package app.bpartners.api.endpoint.rest.controller;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.SesConf;
import app.bpartners.api.endpoint.event.model.ProspectUpdated;
import app.bpartners.api.endpoint.rest.mapper.ProspectJobRestMapper;
import app.bpartners.api.endpoint.rest.mapper.ProspectRestMapper;
import app.bpartners.api.endpoint.rest.model.ProspectStatus;
import app.bpartners.api.endpoint.rest.model.UpdateProspect;
import app.bpartners.api.endpoint.rest.validator.ExtendedProspectUpdateValidator;
import app.bpartners.api.endpoint.rest.validator.ProspectRestValidator;
import app.bpartners.api.model.UserWhiteListed;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.mapper.ProspectMapper;
import app.bpartners.api.repository.ProspectEvaluationJobRepository;
import app.bpartners.api.repository.ProspectRepository;
import app.bpartners.api.repository.expressif.utils.ProspectEvalUtils;
import app.bpartners.api.repository.google.calendar.CalendarApi;
import app.bpartners.api.repository.google.sheets.SheetApi;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.ProspectJpaRepository;
import app.bpartners.api.repository.jpa.UserWhiteListedJpaRepository;
import app.bpartners.api.repository.jpa.model.HProspect;
import app.bpartners.api.service.SnsService;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.customer.CustomerService;
import app.bpartners.api.service.dataprocesser.ProspectDataProcesser;
import app.bpartners.api.service.prospect.ProspectService;
import app.bpartners.api.service.prospect.ProspectStatusService;
import app.bpartners.api.service.user.UserService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ProspectControllerTest {
  EventProducer eventProducerMock = mock();
  ProspectJobRestMapper prospectJobRestMapper = mock();
  ProspectEvalUtils prospectEvalUtilsMock = mock();
  ProspectRepository prospectRepository = mock();
  ProspectDataProcesser prospectDataProcesser = mock();
  AccountHolderJpaRepository accountHolderJpaRepository = mock();
  SesService sesService = mock();
  CustomerService customerService = mock();
  SheetApi sheetApi = mock();
  ProspectMapper prospectMapper = mock();
  ProspectEvaluationJobRepository prospectEvaluationJobRepository = mock();
  SesConf sesConf = mock();
  ProspectStatusService prospectStatusService = mock();
  SnsService snsService = mock();
  UserService userService = mock();
  CalendarApi calendarApi = mock();
  TemplateResolverEngine templateResolverEngine = mock();
  CustomDateFormatter customDateFormatter = mock();
  UserWhiteListedJpaRepository userWhiteListedJpaRepositoryMock = mock();
  ProspectJpaRepository prospectJpaRepositoryMock = mock();

  ProspectRestValidator prospectRestValidator = new ProspectRestValidator();
  ExtendedProspectUpdateValidator extendedProspectUpdateValidator =
      new ExtendedProspectUpdateValidator();
  ProspectRestMapper prospectRestMapper =
      new ProspectRestMapper(prospectRestValidator, extendedProspectUpdateValidator);

  ProspectService prospectService =
      new ProspectService(
          prospectRepository,
          prospectDataProcesser,
          accountHolderJpaRepository,
          sesService,
          customerService,
          sheetApi,
          prospectMapper,
          prospectEvaluationJobRepository,
          eventProducerMock,
          sesConf,
          prospectStatusService,
          snsService,
          userService,
          calendarApi,
          templateResolverEngine,
          customDateFormatter,
          prospectJpaRepositoryMock,
          userWhiteListedJpaRepositoryMock);

  ProspectController subject =
      new ProspectController(
          prospectService,
          prospectRestMapper,
          prospectEvalUtilsMock,
          prospectRestValidator,
          prospectJobRestMapper,
          eventProducerMock);

  @Test
  void exception_when_saving_existing_prospect_on_crupdateProspectsWithEmailCheck() {
    var prospects = List.of(new UpdateProspect().email("old@email.com"));
    var accountHolderId = "accountHolderId";

    when(userWhiteListedJpaRepositoryMock.findByIdAccountHolder(anyString()))
        .thenReturn(Optional.of(userWhiteListed()));
    when(prospectJpaRepositoryMock.findByOldEmailOrNewEmailAndIdAccountHolder(
            anyString(), anyString(), anyString()))
        .thenReturn(List.of(hProspect()));

    assertThrows(
        BadRequestException.class,
        () -> subject.crupdateProspectsWithEmailCheck(accountHolderId, prospects));
  }

  @Test
  void save_not_existing_prospects_and_triggers_events() {
    var accountHolderId = "accountHolderId";
    var emailOne = randomUUID().toString();
    var emailTwo = randomUUID().toString();
    var updates =
        List.of(
            new UpdateProspect().id(emailOne).email(emailOne).status(ProspectStatus.TO_CONTACT),
            new UpdateProspect().id(emailTwo).email(emailTwo).status(ProspectStatus.TO_CONTACT));

    when(userWhiteListedJpaRepositoryMock.findByIdAccountHolder(any()))
        .thenReturn(Optional.empty());
    when(prospectJpaRepositoryMock.findByOldEmailOrNewEmailAndIdAccountHolder(any(), any(), any()))
        .thenReturn(List.of());
    when(prospectRepository.saveAll(anyList()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

    var actual = subject.crupdateProspectsWithEmailCheck(accountHolderId, updates);

    var eventCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(2)).accept(eventCaptor.capture());
    var allEvents = eventCaptor.getAllValues();
    var firstEvent = (ProspectUpdated) allEvents.getFirst().getFirst();
    var secondEvent = (ProspectUpdated) allEvents.getLast().getFirst();

    assertEquals(2, actual.size());
    assertEquals(emailOne, actual.get(0).getEmail());
    assertEquals(emailTwo, actual.get(1).getEmail());
    assertTrue(firstEvent.isNew());
    assertTrue(secondEvent.isNew());
  }

  UserWhiteListed userWhiteListed() {
    return new UserWhiteListed().toBuilder().id("existingUserId").build();
  }

  HProspect hProspect() {
    return new HProspect()
        .toBuilder()
            .id("existingProspectId")
            .oldEmail("old@email.com")
            .newEmail("new@email.com")
            .idAccountHolder("accountHolderOwnerId")
            .build();
  }
}
