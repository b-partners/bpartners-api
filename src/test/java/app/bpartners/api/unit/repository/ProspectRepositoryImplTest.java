package app.bpartners.api.unit.repository;

import app.bpartners.api.endpoint.rest.security.AuthenticatedResourceProvider;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.AnnualRevenueTarget;
import app.bpartners.api.model.BusinessActivity;
import app.bpartners.api.model.Prospect;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.mapper.ProspectMapper;
import app.bpartners.api.repository.AccountHolderRepository;
import app.bpartners.api.repository.SogefiBuildingPermitRepository;
import app.bpartners.api.repository.implementation.ProspectRepositoryImpl;
import app.bpartners.api.repository.jpa.MunicipalityJpaRepository;
import app.bpartners.api.repository.jpa.ProspectJpaRepository;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import app.bpartners.api.repository.jpa.model.HProspect;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitApi;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.BuildingPermit;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.BuildingPermitList;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.SingleBuildingPermit;
import app.bpartners.api.service.AnnualRevenueTargetService;
import app.bpartners.api.service.BusinessActivityService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.OTHER_ACCOUNT_ID;
import static app.bpartners.api.repository.implementation.ProspectRepositoryImpl.ROOFER;
import static app.bpartners.api.repository.implementation.ProspectRepositoryImpl.TILE_LAYER;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

  @BeforeEach
  void setUp() {
    prospectJpaRepositoryMock = mock(ProspectJpaRepository.class);
    prospectMapper = mock(ProspectMapper.class);
    buildingPermitApiMock = mock(BuildingPermitApi.class);
    sogefiBuildingPermitRepositoryMock = mock(SogefiBuildingPermitRepository.class);
    businessActivityServiceMock = mock(BusinessActivityService.class);
    resourceProviderMock = mock(AuthenticatedResourceProvider.class);
    revenueTargetServiceMock = mock(AnnualRevenueTargetService.class);
    accountHolderRepositoryMock = mock(AccountHolderRepository.class);
    municipalityJpaRepositoryMock = mock(MunicipalityJpaRepository.class);
    subject =
        new ProspectRepositoryImpl(prospectJpaRepositoryMock, prospectMapper, buildingPermitApiMock,
            sogefiBuildingPermitRepositoryMock, businessActivityServiceMock, resourceProviderMock,
            revenueTargetServiceMock, accountHolderRepositoryMock, municipalityJpaRepositoryMock);
    when(revenueTargetServiceMock.getByYear(JOE_DOE_ACCOUNT_ID, 2023)).thenReturn(
        Optional.ofNullable(
            AnnualRevenueTarget.builder()
                .amountTarget(parseFraction(150000))
                .amountAttempted(parseFraction(32000))
                .accountHolder(HAccountHolder.builder().build())
                .build()));
    when(revenueTargetServiceMock.getByYear(OTHER_ACCOUNT_ID, 2023))
        .thenReturn(Optional.ofNullable(AnnualRevenueTarget.builder()
            .amountTarget(parseFraction(150000))
            .amountAttempted(parseFraction(30000))
            .accountHolder(HAccountHolder
                .builder()
                .build())
            .build()));
    AccountHolder accountHolder = AccountHolder.builder()
        .id(JOE_DOE_ACCOUNT_ID)
        .townCode(67890)
        .build();
    when(accountHolderRepositoryMock.findById(JOE_DOE_ACCOUNT_ID)).thenReturn(accountHolder);
    when(buildingPermitApiMock.getData(String.valueOf(accountHolder.getTownCode())))
        .thenReturn(BuildingPermitList.builder()
            .records(List.of(BuildingPermit.builder().fileId(1L).build()))
            .limit(1)
            .total(1)
            .build());
    when(buildingPermitApiMock.getOne(any()))
        .thenReturn(SingleBuildingPermit.singleBuildingPermitBuilder()
            .fileId(1L)
            .fileRef("fad")
            .ref("fda")
            .build());
    when(prospectJpaRepositoryMock.findAllByIdAccountHolderAndTownCodeIsIn(JOE_DOE_ACCOUNT_ID,
        List.of(1324))).thenReturn(
        List.of(HProspect.builder()
            .build()));
  }

  void setUp_sogefi_api_is_not_called() {
    BusinessActivity nonSogefiProspectorBusinessActivity = BusinessActivity.builder()
        .primaryActivity("primary")
        .secondaryActivity("secondary")
        .build();

    when(prospectMapper.toDomain(HProspect.builder()
        .id(JOE_DOE_ACCOUNT_ID)
        .build(), false))
        .thenReturn(Prospect.builder().build());
    when(businessActivityServiceMock.findByAccountHolderId(JOE_DOE_ACCOUNT_ID))
        .thenReturn(nonSogefiProspectorBusinessActivity);
  }

  void setUp_sogefi_api_is_called() {
    BusinessActivity sogefiProspectorBusinessActivity = BusinessActivity.builder()
        .primaryActivity(TILE_LAYER)
        .secondaryActivity(ROOFER)
        .build();
    when(businessActivityServiceMock.findByAccountHolderId(JOE_DOE_ACCOUNT_ID))
        .thenReturn(sogefiProspectorBusinessActivity);
    when(prospectMapper.toDomain(HProspect.builder()
        .id(JOE_DOE_ACCOUNT_ID)
        .build(), true))
        .thenReturn(Prospect.builder().build());

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

  @Test
  void sogefi_api_is_not_called() {
    setUp_sogefi_api_is_not_called();

    subject.findAllByIdAccountHolder(JOE_DOE_ACCOUNT_ID);

    verify(accountHolderRepositoryMock).findById(JOE_DOE_ACCOUNT_ID);
    verify(prospectJpaRepositoryMock).findAllByIdAccountHolderAndTownCodeIsIn(any(String.class),
        any());
    verify(buildingPermitApiMock, never()).getData(any());
    verify(buildingPermitApiMock, never()).getOne(any());
    verify(sogefiBuildingPermitRepositoryMock, never()).saveByBuildingPermit(any(), any(), any());
  }

  @Test
  void sogefi_api_is_called() {
    setUp_sogefi_api_is_called();

    subject.findAllByIdAccountHolder(JOE_DOE_ACCOUNT_ID);

    verify(buildingPermitApiMock).getData(any());
    verify(buildingPermitApiMock).getOne(any());
  }

  @Test
  void should_throw_bad_request_exception() {
    AccountHolder accountHolderWithoutTownCode = AccountHolder.builder()
        .id(JOE_DOE_ACCOUNT_ID)
        .townCode(null)
        .build();

    when(accountHolderRepositoryMock.findById(JOE_DOE_ACCOUNT_ID)).thenReturn(
        accountHolderWithoutTownCode);

    assertThrows(BadRequestException.class,
        () -> subject.findAllByIdAccountHolder(JOE_DOE_ACCOUNT_ID),
        "AccountHolder.id=" + JOE_DOE_ACCOUNT_ID + " is missing the "
            + "required property town " + "code");
  }
}
