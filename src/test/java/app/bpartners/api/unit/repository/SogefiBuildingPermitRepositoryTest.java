package app.bpartners.api.unit.repository;

import static app.bpartners.api.endpoint.rest.model.ProspectStatus.TO_CONTACT;
import static app.bpartners.api.integration.conf.utils.TestUtils.ACCOUNTHOLDER_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.implementation.SogefiBuildingPermitRepositoryImpl;
import app.bpartners.api.repository.jpa.ProspectJpaRepository;
import app.bpartners.api.repository.jpa.SogefiBuildingPermitJpaRepository;
import app.bpartners.api.repository.jpa.model.HProspect;
import app.bpartners.api.repository.jpa.model.HProspectStatusHistory;
import app.bpartners.api.repository.jpa.model.HSogefiBuildingPermitProspect;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.Applicant;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.BuildingPermit;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.GeoJson;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.SingleBuildingPermit;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.SogefiInformation;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

// TODO: make test pass with prospect history statuses
class SogefiBuildingPermitRepositoryTest {
  public static final String PROSPECT_NAME = "name";
  public static final String PROSPECT_ADDRESS = "address";
  private static final long ID_SOGEFI = 1000;
  private static final String PROSPECT1_ID = "prospect1_id";
  private static final String SOGEFI_PROSPECT_ID = "sogefi_prospect_id";
  private SogefiBuildingPermitRepositoryImpl subject;
  private SogefiBuildingPermitJpaRepository jpaRepositoryMock;
  private ProspectJpaRepository prospectJpaRepositoryMock;

  private static Applicant applicant() {
    return Applicant.builder().name(PROSPECT_NAME).address(PROSPECT_ADDRESS).build();
  }

  private static SogefiInformation sogefiInformation() {
    return SogefiInformation.builder().permitApplicant(applicant()).build();
  }

  @BeforeEach
  void setUp() {
    jpaRepositoryMock = mock(SogefiBuildingPermitJpaRepository.class);
    prospectJpaRepositoryMock = mock(ProspectJpaRepository.class);
    subject = new SogefiBuildingPermitRepositoryImpl(jpaRepositoryMock, prospectJpaRepositoryMock);
    reset(jpaRepositoryMock);
    reset(prospectJpaRepositoryMock);
    when(prospectJpaRepositoryMock.save(any()))
        .thenAnswer(
            i -> {
              HProspect entity = i.getArgument(0);
              entity.setId(PROSPECT1_ID);
              return entity;
            });
  }

  HSogefiBuildingPermitProspect sogefiBuildingPermitProspect() {
    return HSogefiBuildingPermitProspect.builder()
        .id(SOGEFI_PROSPECT_ID)
        .idProspect(PROSPECT1_ID)
        .idSogefi(ID_SOGEFI)
        .geojsonType("Point")
        .geojsonLongitude(2.6249715936056646)
        .geojsonLatitude(47.82333624900505)
        .build();
  }

  BuildingPermit buildingPermit() {
    return BuildingPermit.builder()
        .insee("1000")
        .type("PC")
        .ref("ref")
        .fileId(ID_SOGEFI)
        .fileRef("fileref")
        .longType("Permis de construire")
        .year(2023)
        .suffix(null)
        .geoJson(null)
        .centroidGeoJson(
            GeoJson.<List<Object>>builder().type("Point").coordinates(List.of(1.0, 23.0)).build())
        .build();
  }

  SingleBuildingPermit singleBuildingPermit() {
    return SingleBuildingPermit.singleBuildingPermitBuilder()
        .sogefiInformation(sogefiInformation())
        .insee("92001")
        .build();
  }

  HSogefiBuildingPermitProspect expectedSavedSogefiProspect() {
    return HSogefiBuildingPermitProspect.builder()
        .id(SOGEFI_PROSPECT_ID)
        .idSogefi(buildingPermit().getFileId())
        .idProspect(PROSPECT1_ID)
        .geojsonType(buildingPermit().getCentroidGeoJson().getType())
        .geojsonLongitude((Double) buildingPermit().getCentroidGeoJson().getCoordinates().get(0))
        .geojsonLatitude((Double) buildingPermit().getCentroidGeoJson().getCoordinates().get(1))
        .build();
  }

  HProspect expectedSavedProspect() {
    return HProspect.builder()
        .id(PROSPECT1_ID)
        .oldName(PROSPECT_NAME)
        .oldAddress(PROSPECT_ADDRESS)
        .statusHistories(defaultStatusHistoriesEntity())
        .idAccountHolder(ACCOUNTHOLDER_ID)
        .townCode(92001)
        .rating(-1.0)
        .build();
  }

  HProspect toUpdateProspect() {
    return HProspect.builder()
        .id(PROSPECT1_ID)
        .oldName("some name")
        .oldAddress("some address")
        .statusHistories(defaultStatusHistoriesEntity())
        .idAccountHolder(ACCOUNTHOLDER_ID)
        .townCode(92002)
        .build();
  }

  private static List<HProspectStatusHistory> defaultStatusHistoriesEntity() {
    return List.of(
        HProspectStatusHistory.builder()
            .id("TODO")
            .status(TO_CONTACT)
            .updatedAt(Instant.now().truncatedTo(ChronoUnit.MINUTES))
            .build());
  }

  @Test
  void save_by_building_permit_creates_new_prospect_ok() {
    when(jpaRepositoryMock.findByIdSogefi(ID_SOGEFI)).thenReturn(Optional.empty());
    HSogefiBuildingPermitProspect expected = expectedSavedSogefiProspect();
    expected.setId(null);
    ArgumentCaptor<HSogefiBuildingPermitProspect> sogefiProspectEntityCaptor =
        ArgumentCaptor.forClass(HSogefiBuildingPermitProspect.class);
    ArgumentCaptor<HProspect> prospectEntityArgumentCaptor =
        ArgumentCaptor.forClass(HProspect.class);

    subject.saveByBuildingPermit(ACCOUNTHOLDER_ID, buildingPermit(), singleBuildingPermit());

    verify(prospectJpaRepositoryMock, times(1)).save(prospectEntityArgumentCaptor.capture());
    verify(jpaRepositoryMock, times(1)).save(sogefiProspectEntityCaptor.capture());

    HProspect prospectEntityArgumentCaptorValue = prospectEntityArgumentCaptor.getValue();
    assertTrue(
        prospectEntityArgumentCaptorValue.getStatusHistories().stream()
            .anyMatch(prospect -> prospect.getStatus().equals(TO_CONTACT)));
    prospectEntityArgumentCaptorValue.setStatusHistories(null);
    assertEquals(
        expectedSavedProspect().toBuilder()
            .lastEvaluationDate(prospectEntityArgumentCaptorValue.getLastEvaluationDate())
            .statusHistories(null)
            .build(),
        prospectEntityArgumentCaptorValue);
    assertEquals(expected, sogefiProspectEntityCaptor.getValue());
  }

  @Test
  void save_by_building_permit_updates_existing_prospect_ok() {
    when(jpaRepositoryMock.findByIdSogefi(ID_SOGEFI))
        .thenReturn(Optional.of(sogefiBuildingPermitProspect()));
    when(prospectJpaRepositoryMock.findById(PROSPECT1_ID))
        .thenReturn(Optional.of(toUpdateProspect()));
    HProspect expectedProspect = expectedSavedProspect();
    expectedProspect.setStatusHistories(defaultStatusHistoriesEntity());
    ArgumentCaptor<HSogefiBuildingPermitProspect> sogefiProspectEntityCaptor =
        ArgumentCaptor.forClass(HSogefiBuildingPermitProspect.class);
    ArgumentCaptor<HProspect> prospectEntityArgumentCaptor =
        ArgumentCaptor.forClass(HProspect.class);

    subject.saveByBuildingPermit(ACCOUNTHOLDER_ID, buildingPermit(), singleBuildingPermit());

    verify(prospectJpaRepositoryMock, times(1)).save(prospectEntityArgumentCaptor.capture());
    verify(jpaRepositoryMock, times(1)).save(sogefiProspectEntityCaptor.capture());

    HProspect prospectCaptureValue = prospectEntityArgumentCaptor.getValue();
    assertEquals(
        expectedProspect.toBuilder()
            .lastEvaluationDate(prospectCaptureValue.getLastEvaluationDate())
            .build(),
        prospectCaptureValue);
    assertEquals(expectedSavedSogefiProspect(), sogefiProspectEntityCaptor.getValue());
  }

  @Test
  void save_by_building_permit_ko() {
    when(prospectJpaRepositoryMock.findById(any())).thenReturn(Optional.empty());
    when(jpaRepositoryMock.findByIdSogefi(ID_SOGEFI))
        .thenReturn(Optional.of(sogefiBuildingPermitProspect()));
    BuildingPermit buildingPermit = buildingPermit();
    SingleBuildingPermit singleBuildingPermit = singleBuildingPermit();

    assertThrows(
        ApiException.class,
        () -> subject.saveByBuildingPermit(ACCOUNTHOLDER_ID, buildingPermit, singleBuildingPermit),
        "HProspect.id="
            + PROSPECT1_ID
            + " was not found but it was linked with "
            + "HSogefiBuildingPermitProspect.id="
            + SOGEFI_PROSPECT_ID);
  }
}
