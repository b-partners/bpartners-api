package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.model.Geojson;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.SogefiBuildingPermitRepository;
import app.bpartners.api.repository.jpa.ProspectJpaRepository;
import app.bpartners.api.repository.jpa.SogefiBuildingPermitJpaRepository;
import app.bpartners.api.repository.jpa.model.HProspect;
import app.bpartners.api.repository.jpa.model.HSogefiBuildingPermitProspect;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.Applicant;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.BuildingPermit;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.SingleBuildingPermit;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import static app.bpartners.api.endpoint.rest.model.ProspectStatus.TO_CONTACT;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

@Repository
@AllArgsConstructor
public class SogefiBuildingPermitRepositoryImpl implements SogefiBuildingPermitRepository {
  public static final int COORDINATES_LENGTH = 2;
  public static final int LONGITUDE_INDEX = 0;
  public static final int LATITUDE_INDEX = 1;
  public static final double DEFAULT_RATING = -1.0;
  private final SogefiBuildingPermitJpaRepository jpaRepository;
  private final ProspectJpaRepository prospectJpaRepository;

  @Transactional
  @Override
  public void saveByBuildingPermit(String idAccountHolder, BuildingPermit buildingPermit,
                                   SingleBuildingPermit singleBuildingPermit) {
    Optional<HSogefiBuildingPermitProspect> persisted =
        jpaRepository.findByIdSogefi(buildingPermit.getFileId());
    List<Object> coordinates = buildingPermit.getCentroidGeoJson() == null ? List.of() :
        buildingPermit.getCentroidGeoJson().getCoordinates();
    //we only save prospect data with coordinates because either way we would have invalid
    // prospects
    if (coordinates.size() == COORDINATES_LENGTH) {
      Applicant applicant = singleBuildingPermit.getSogefiInformation().getPermitApplicant();
      HProspect prospectEntityToSave = HProspect.builder()
          .oldName(applicant.getName())
          .oldAddress(applicant.getAddress())
          .idAccountHolder(idAccountHolder)
          .townCode(Integer.parseInt(singleBuildingPermit.getInsee()))
          .rating(DEFAULT_RATING)
          .lastEvaluationDate(Instant.now())
          .build();
      if (persisted.isEmpty()) {
        prospectEntityToSave.setStatus(TO_CONTACT);
      } else {
        Optional<HProspect> persistedProspectEntity =
            prospectJpaRepository.findById(persisted.get().getIdProspect());
        if (persistedProspectEntity.isPresent()) {
          prospectEntityToSave.setId(persistedProspectEntity.get().getId());
          prospectEntityToSave.setStatus(persistedProspectEntity.get().getStatus());
          prospectEntityToSave.setOldPhone(persistedProspectEntity.get().getOldPhone());
          prospectEntityToSave.setOldPhone(persistedProspectEntity.get().getOldPhone());
        } else {
          throw new ApiException(SERVER_EXCEPTION,
              "HProspect.id=" + persisted.get().getIdProspect()
                  + " was not found but it was linked with HSogefiBuildingPermitProspect.id="
                  + persisted.get().getId());
        }
      }
      HProspect savedProspectEntity = prospectJpaRepository.save(prospectEntityToSave);
      jpaRepository.save(HSogefiBuildingPermitProspect.builder()
          .id(persisted.map(HSogefiBuildingPermitProspect::getId).orElse(null))
          .idSogefi(buildingPermit.getFileId())
          .idProspect(savedProspectEntity.getId())
          .geojsonType(buildingPermit.getCentroidGeoJson().getType())
          .geojsonLongitude((Double) coordinates.get(LONGITUDE_INDEX))
          .geojsonLatitude((Double) coordinates.get(LATITUDE_INDEX))
          .build());
    }
  }

  @Override
  public Geojson findLocationByIdProspect(String idProspect) {
    Optional<HSogefiBuildingPermitProspect> sogefi =
        jpaRepository.findByIdProspect(idProspect);
    return sogefi.map(sogefiProspect -> new Geojson()
            .type(sogefiProspect.getGeojsonType())
            .longitude(sogefiProspect.getGeojsonLongitude())
            .latitude(sogefiProspect.getGeojsonLatitude()))
        .orElse(null);
  }
}
