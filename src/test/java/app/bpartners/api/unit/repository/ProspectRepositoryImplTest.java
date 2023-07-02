package app.bpartners.api.unit.repository;

import app.bpartners.api.endpoint.rest.security.AuthenticatedResourceProvider;
import app.bpartners.api.model.AnnualRevenueTarget;
import app.bpartners.api.model.mapper.ProspectMapper;
import app.bpartners.api.repository.AccountHolderRepository;
import app.bpartners.api.repository.SogefiBuildingPermitRepository;
import app.bpartners.api.repository.expressif.ExpressifApi;
import app.bpartners.api.repository.implementation.ProspectRepositoryImpl;
import app.bpartners.api.repository.jpa.MunicipalityJpaRepository;
import app.bpartners.api.repository.jpa.ProspectJpaRepository;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitApi;
import app.bpartners.api.service.AnnualRevenueTargetService;
import app.bpartners.api.service.BusinessActivityService;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.OTHER_ACCOUNT_ID;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProspectRepositoryImplTest {

  ProspectRepositoryImpl subject;
  ProspectJpaRepository prospectJpaRepositoryMock;
  ProspectMapper prospectMapper;
  BuildingPermitApi buildingPermitApiMock;
  SogefiBuildingPermitRepository sogefiBuildingPermitRepositoryMock;
  BusinessActivityService businessActivityServiceMock;
  AuthenticatedResourceProvider resourceProviderMock;
  AnnualRevenueTargetService revenueTargetServiceMock;
  AccountHolderRepository accountHolderRepositoryMock;
  MunicipalityJpaRepository municipalityJpaRepositoryMock;
  ExpressifApi expressifApiMock;

  @BeforeEach
  void setUp() {
    prospectJpaRepositoryMock = mock(ProspectJpaRepository.class);
    prospectMapper = new ProspectMapper(resourceProviderMock, sogefiBuildingPermitRepositoryMock,
        prospectJpaRepositoryMock);
    buildingPermitApiMock = mock(BuildingPermitApi.class);
    sogefiBuildingPermitRepositoryMock = mock(SogefiBuildingPermitRepository.class);
    businessActivityServiceMock = mock(BusinessActivityService.class);
    resourceProviderMock = mock(AuthenticatedResourceProvider.class);
    revenueTargetServiceMock = mock(AnnualRevenueTargetService.class);
    accountHolderRepositoryMock = mock(AccountHolderRepository.class);
    municipalityJpaRepositoryMock = mock(MunicipalityJpaRepository.class);
    expressifApiMock = mock(ExpressifApi.class);

    subject =
        new ProspectRepositoryImpl(prospectJpaRepositoryMock, prospectMapper, buildingPermitApiMock,
            sogefiBuildingPermitRepositoryMock, businessActivityServiceMock, resourceProviderMock,
            revenueTargetServiceMock, accountHolderRepositoryMock, municipalityJpaRepositoryMock,
            expressifApiMock);
    when(revenueTargetServiceMock.getByYear(JOE_DOE_ACCOUNT_ID, 2023)).thenReturn(
        Optional.ofNullable(
            AnnualRevenueTarget.builder()
                .amountTarget(parseFraction(150000))
                .amountAttempted(parseFraction(32000))
                .idAccountHolder(EMPTY)
                .build()));
    when(revenueTargetServiceMock.getByYear(OTHER_ACCOUNT_ID, 2023))
        .thenReturn(Optional.ofNullable(AnnualRevenueTarget.builder()
            .amountTarget(parseFraction(150000))
            .amountAttempted(parseFraction(30000))
            .idAccountHolder(EMPTY)
            .build()));
  }

  @Test
  void needsProspects_false() {
    boolean needProspect = subject.needsProspects(JOE_DOE_ACCOUNT_ID, LocalDate.parse(
        "2023-03-15"));

    assertFalse(needProspect);
  }

  @Test
  void needsProspects_true() {
    boolean needProspect = subject.needsProspects(OTHER_ACCOUNT_ID, LocalDate.parse(
        "2023-03-15"));

    assertTrue(needProspect);
  }
}
