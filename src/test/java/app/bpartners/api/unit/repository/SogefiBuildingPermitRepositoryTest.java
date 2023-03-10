package app.bpartners.api.unit.repository;

import app.bpartners.api.endpoint.rest.model.ProspectStatus;
import app.bpartners.api.repository.implementation.SogefiBuildingPermitRepositoryImpl;
import app.bpartners.api.repository.jpa.ProspectJpaRepository;
import app.bpartners.api.repository.jpa.SogefiBuildingPermitJpaRepository;
import app.bpartners.api.repository.jpa.model.HProspect;
import app.bpartners.api.repository.jpa.model.HSogefiBuildingPermitProspect;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.Applicant;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.BuildingPermit;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.GeoJson;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.SingleBuildingPermit;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.SogefiInformation;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static app.bpartners.api.integration.conf.TestUtils.SWAN_ACCOUNTHOLDER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SogefiBuildingPermitRepositoryTest {
  public static final String PROSPECT_NAME = "name";
  public static final String PROSPECT_ADDRESS = "address";
  private static final long ID_SOGEFI = 1000;
  private static final String PROSPECT1_ID = "prospect1_id";
  private SogefiBuildingPermitRepositoryImpl subject;
  private SogefiBuildingPermitJpaRepository jpaRepositoryMock;
  private ProspectJpaRepository prospectJpaRepositoryMock;

  private static Applicant applicant() {
    return Applicant.builder()
        .name(PROSPECT_NAME)
        .address(PROSPECT_ADDRESS)
        .build();
  }

  private static SogefiInformation sogefiInformation() {
    return SogefiInformation.builder()
        .permitApplicant(applicant())
        .build();
  }

  @BeforeEach
  void setUp() {
    jpaRepositoryMock = mock(SogefiBuildingPermitJpaRepository.class);
    prospectJpaRepositoryMock = mock(ProspectJpaRepository.class);
    subject = new SogefiBuildingPermitRepositoryImpl(jpaRepositoryMock, prospectJpaRepositoryMock);
  }

  HSogefiBuildingPermitProspect sogefiBuildingPermitProspect() {
    return HSogefiBuildingPermitProspect.builder()
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
        .centroidGeoJson(GeoJson.<List<Object>>builder()
            .type("Point")
            .coordinates(List.of(1.0, 23.0))
            .build())
        .build();
  }

  SingleBuildingPermit singleBuildingPermit() {
    return SingleBuildingPermit.singleBuildingPermitBuilder()
        .sogefiInformation(sogefiInformation())
        .build();
  }

  HSogefiBuildingPermitProspect expectedSavedSogefiProspect() {
    return HSogefiBuildingPermitProspect.builder()
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
        .name(PROSPECT_NAME)
        .address(PROSPECT_ADDRESS)
        .status(ProspectStatus.TO_CONTACT)
        .idAccountHolder(SWAN_ACCOUNTHOLDER_ID)
        .build();
  }

  @Test
  void save_by_building_permit_ok() {
    reset(jpaRepositoryMock);
    when(jpaRepositoryMock.findByIdSogefi(ID_SOGEFI)).thenReturn(Optional.empty());
    when(prospectJpaRepositoryMock.save(any())).thenAnswer(i -> {
      HProspect entity = i.getArgument(0);
      entity.setId(PROSPECT1_ID);
      return entity;
    });
    ArgumentCaptor<HSogefiBuildingPermitProspect> sogefiProspectEntityCaptor =
        ArgumentCaptor.forClass(HSogefiBuildingPermitProspect.class);
    ArgumentCaptor<HProspect> prospectEntityArgumentCaptor =
        ArgumentCaptor.forClass(HProspect.class);

    subject.saveByBuildingPermit(SWAN_ACCOUNTHOLDER_ID, buildingPermit(),
        singleBuildingPermit());

    verify(prospectJpaRepositoryMock, times(1)).save(prospectEntityArgumentCaptor.capture());
    verify(jpaRepositoryMock, times(1)).save(sogefiProspectEntityCaptor.capture());

    assertEquals(expectedSavedProspect(), prospectEntityArgumentCaptor.getValue());
    assertEquals(expectedSavedSogefiProspect(), sogefiProspectEntityCaptor.getValue());
  }

  @Test
  void save_by_building_permit_do_nothing() {
    reset(jpaRepositoryMock);
    when(jpaRepositoryMock.findByIdSogefi(ID_SOGEFI)).thenReturn(Optional.of(
        sogefiBuildingPermitProspect()));

    subject.saveByBuildingPermit(SWAN_ACCOUNTHOLDER_ID,
        buildingPermit(), singleBuildingPermit());

    verify(jpaRepositoryMock, never()).save(any());
    verify(prospectJpaRepositoryMock, never()).save(any());
  }
}
