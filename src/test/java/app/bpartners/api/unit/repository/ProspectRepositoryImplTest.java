package app.bpartners.api.unit.repository;

import static app.bpartners.api.endpoint.rest.model.ProspectStatus.TO_CONTACT;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.OTHER_ACCOUNT_ID;
import static app.bpartners.api.repository.expressif.fact.NewIntervention.OldCustomer.OldCustomerType.INDIVIDUAL;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static java.time.Instant.now;
import static java.time.Month.MARCH;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.rest.model.ProspectFeedback;
import app.bpartners.api.endpoint.rest.security.AuthenticatedResourceProvider;
import app.bpartners.api.model.AnnualRevenueTarget;
import app.bpartners.api.model.mapper.ProspectEvalMapper;
import app.bpartners.api.model.mapper.ProspectMapper;
import app.bpartners.api.repository.AccountHolderRepository;
import app.bpartners.api.repository.SogefiBuildingPermitRepository;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.expressif.ExpressifApi;
import app.bpartners.api.repository.expressif.ProspectEval;
import app.bpartners.api.repository.expressif.ProspectResult;
import app.bpartners.api.repository.expressif.fact.NewIntervention;
import app.bpartners.api.repository.expressif.model.OutputValue;
import app.bpartners.api.repository.implementation.ProspectRepositoryImpl;
import app.bpartners.api.repository.jpa.MunicipalityJpaRepository;
import app.bpartners.api.repository.jpa.ProspectEvalInfoJpaRepository;
import app.bpartners.api.repository.jpa.ProspectJpaRepository;
import app.bpartners.api.repository.jpa.model.HProspect;
import app.bpartners.api.repository.jpa.model.HProspectEval;
import app.bpartners.api.repository.jpa.model.HProspectEvalInfo;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitApi;
import app.bpartners.api.service.accountholder.BusinessActivityService;
import app.bpartners.api.service.target.AnnualRevenueTargetService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProspectRepositoryImplTest {

  ProspectRepositoryImpl subject;
  ProspectJpaRepository jpaRepositoryMock;
  ProspectMapper prospectMapper;
  BuildingPermitApi buildingPermitApiMock;
  SogefiBuildingPermitRepository sogefiBuildingPermitRepositoryMock;
  BusinessActivityService businessActivityServiceMock;
  AuthenticatedResourceProvider resourceProviderMock;
  AnnualRevenueTargetService revenueTargetServiceMock;
  AccountHolderRepository accountHolderRepositoryMock;
  MunicipalityJpaRepository municipalityJpaRepositoryMock;
  ExpressifApi expressifApiMock;
  ProspectEvalMapper evalMapperMock;
  ProspectEvalInfoJpaRepository evalRepositoryMock;
  BanApi banApiMock;
  EntityManager em;

  @BeforeEach
  void setUp() {
    jpaRepositoryMock = mock(ProspectJpaRepository.class);
    banApiMock = mock(BanApi.class);
    prospectMapper = new ProspectMapper(banApiMock, new CustomDateFormatter());
    buildingPermitApiMock = mock(BuildingPermitApi.class);
    sogefiBuildingPermitRepositoryMock = mock(SogefiBuildingPermitRepository.class);
    businessActivityServiceMock = mock(BusinessActivityService.class);
    resourceProviderMock = mock(AuthenticatedResourceProvider.class);
    revenueTargetServiceMock = mock(AnnualRevenueTargetService.class);
    accountHolderRepositoryMock = mock(AccountHolderRepository.class);
    municipalityJpaRepositoryMock = mock(MunicipalityJpaRepository.class);
    expressifApiMock = mock(ExpressifApi.class);
    evalMapperMock = mock(ProspectEvalMapper.class);
    evalRepositoryMock = mock(ProspectEvalInfoJpaRepository.class);
    em = mock(EntityManager.class);

    subject =
        new ProspectRepositoryImpl(
            jpaRepositoryMock,
            prospectMapper,
            businessActivityServiceMock,
            resourceProviderMock,
            revenueTargetServiceMock,
            accountHolderRepositoryMock,
            expressifApiMock,
            evalMapperMock,
            evalRepositoryMock,
            em,
            sogefiBuildingPermitRepositoryMock);
    when(revenueTargetServiceMock.getByYear(JOE_DOE_ACCOUNT_ID, Year.now().getValue()))
        .thenReturn(
            Optional.ofNullable(
                AnnualRevenueTarget.builder()
                    .amountTarget(parseFraction(150000))
                    .amountAttempted(parseFraction(32000))
                    .idAccountHolder(EMPTY)
                    .build()));
    when(revenueTargetServiceMock.getByYear(OTHER_ACCOUNT_ID, Year.now().getValue()))
        .thenReturn(
            Optional.ofNullable(
                AnnualRevenueTarget.builder()
                    .amountTarget(parseFraction(150000))
                    .amountAttempted(parseFraction(30000))
                    .idAccountHolder(EMPTY)
                    .build()));
  }

  @Test
  void evaluate() {
    Query queryMock = mock(Query.class);
    when(queryMock.getSingleResult()).thenReturn(1L);
    when(em.createNativeQuery("select nextval('prospect_eval_info_ref_seq');"))
        .thenReturn(queryMock);
    var outputValueProspectNotation =
        OutputValue.builder().name("Notation du prospect").value(1.1).build();
    when(expressifApiMock.process(any())).thenReturn(List.of(outputValueProspectNotation));
    var lastEval = HProspectEval.builder().build();
    when(evalMapperMock.toInfoEntity(any(), any(), anyDouble(), anyDouble())).thenReturn(lastEval);
    var hProspectEvalInfo =
        HProspectEvalInfo.builder()
            .id("prospectEvalNewInterventionId")
            .prospectEvals(new ArrayList<>())
            .build();
    when(evalRepositoryMock.findById(any())).thenReturn(Optional.of(hProspectEvalInfo));
    when(evalRepositoryMock.saveAll(anyList())).thenReturn(List.of(hProspectEvalInfo));
    var prospectResult = ProspectResult.builder().build();
    when(evalMapperMock.toResultDomain(any())).thenReturn(prospectResult);
    var oldCustomerNewIntervention =
        NewIntervention.OldCustomer.builder()
            .professionalType("professionalType")
            .type(INDIVIDUAL)
            .distNewIntAndOldCustomer(10.10)
            .build();
    var depaRuleNewIntervention =
        NewIntervention.builder()
            .planned(true)
            .interventionType("interventionType")
            .infestationType("infestationType")
            .distNewIntAndProspect(10.10)
            .oldCustomer(oldCustomerNewIntervention)
            .build();
    var prospectEvalNewIntervention =
        ProspectEval.builder()
            .id("prospectEvalNewInterventionId")
            .lockSmith(true)
            .antiHarm(true)
            .disinfection(true)
            .ratRemoval(false)
            .professionalCustomer(true)
            .particularCustomer(false)
            .depaRule(depaRuleNewIntervention)
            .build();

    var actual = subject.evaluate(List.of(prospectEvalNewIntervention));

    assertEquals(List.of(prospectResult), actual);
    verify(evalRepositoryMock).findById(anyString());
    verify(em).createNativeQuery("select nextval('prospect_eval_info_ref_seq');");
    verify(queryMock).getSingleResult();
    assertEquals(1, hProspectEvalInfo.getProspectEvals().size());
    assertEquals(lastEval, hProspectEvalInfo.getProspectEvals().get(0));
  }

  @Test
  void find_all_by_status() {
    var hProspect =
        HProspect.builder()
            .posLatitude(2.2)
            .posLongitude(3.3)
            .id("id")
            .firstName("firstName")
            .idJob("idJob")
            .idAccountHolder("accountHolderId")
            .newEmail("newEmail")
            .oldName("oldName")
            .managerName("managerName")
            .newPhone("newPhone")
            .oldAddress("oldPhone")
            .statusHistories(List.of())
            .townCode(123)
            .lastEvaluationDate(now())
            .comment("comment")
            .defaultComment("defaultComment")
            .prospectFeedback(ProspectFeedback.INTERESTED)
            .idInvoice("idInvoice")
            .latestOldHolder("latestOldHolder")
            .build();
    when(jpaRepositoryMock.findAllByStatus(any())).thenReturn(List.of(hProspect));

    var actual = subject.findAllByStatus(TO_CONTACT);

    assertEquals(hProspect.getId(), actual.getFirst().getId());
  }

  @Test
  void needsProspects_false() {
    boolean needProspect =
        subject.needsProspects(
            JOE_DOE_ACCOUNT_ID, LocalDate.of(Year.now().getValue(), MARCH.getValue(), 15));

    assertFalse(needProspect);
  }

  @Test
  void needsProspects_true() {
    boolean needProspect =
        subject.needsProspects(
            OTHER_ACCOUNT_ID, LocalDate.of(Year.now().getValue(), MARCH.getValue(), 15));

    assertTrue(needProspect);
  }
}
