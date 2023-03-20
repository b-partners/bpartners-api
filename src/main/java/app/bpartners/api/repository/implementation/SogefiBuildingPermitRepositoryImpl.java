package app.bpartners.api.repository.implementation;

import app.bpartners.api.repository.SogefiBuildingPermitRepository;
import app.bpartners.api.repository.jpa.ProspectJpaRepository;
import app.bpartners.api.repository.jpa.SogefiBuildingPermitJpaRepository;
import app.bpartners.api.repository.jpa.model.HProspect;
import app.bpartners.api.repository.jpa.model.HSogefiBuildingPermitProspect;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.Applicant;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.BuildingPermit;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.SingleBuildingPermit;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import static app.bpartners.api.endpoint.rest.model.ProspectStatus.TO_CONTACT;

@Repository
@AllArgsConstructor
public class SogefiBuildingPermitRepositoryImpl implements SogefiBuildingPermitRepository {
  public static final int COORDINATES_LENGTH = 2;
  public static final int LONGITUDE_INDEX = 0;
  public static final int LATITUDE_INDEX = 1;
  private final SogefiBuildingPermitJpaRepository jpaRepository;
  private final ProspectJpaRepository prospectJpaRepository;

  @Transactional
  @Override
  public void saveByBuildingPermit(String idAccountHolder, BuildingPermit buildingPermit,
                                   SingleBuildingPermit singleBuildingPermit) {
    //todo: handle updates from sogefi's database
    Optional<HSogefiBuildingPermitProspect> sogefi =
        jpaRepository.findByIdSogefi(buildingPermit.getFileId());
    if (sogefi.isEmpty()) {
      List<Object> coordinates = buildingPermit.getCentroidGeoJson() == null ? List.of() :
          buildingPermit.getCentroidGeoJson().getCoordinates();
      //we only save prospect data with coordinates because either way we would have invalid
      // prospects
      if (coordinates.size() == COORDINATES_LENGTH) {
        Applicant applicant = singleBuildingPermit.getSogefiInformation().getPermitApplicant();
        HProspect prospectEntity = prospectJpaRepository.save(
            HProspect.builder()
                .name(applicant.getName())
                .address(applicant.getAddress())
                .status(TO_CONTACT)
                .idAccountHolder(idAccountHolder)
                .build()
        );
        jpaRepository.save(HSogefiBuildingPermitProspect.builder()
            .idSogefi(buildingPermit.getFileId())
            .idProspect(prospectEntity.getId())
            .geojsonType(buildingPermit.getCentroidGeoJson().getType())
            .geojsonLongitude((Double) coordinates.get(LONGITUDE_INDEX))
            .geojsonLatitude((Double) coordinates.get(LATITUDE_INDEX))
            .build());
      }
    }
  }

  @Override
  public HSogefiBuildingPermitProspect findByIdProspect(String idProspect) {
    Optional<HSogefiBuildingPermitProspect> sogefi =
        jpaRepository.findByIdProspect(idProspect);
    return sogefi.orElse(null);
  }
}
