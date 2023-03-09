package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.Prospect;
import app.bpartners.api.model.mapper.ProspectMapper;
import app.bpartners.api.repository.ProspectRepository;
import app.bpartners.api.repository.SogefiBuildingPermitRepository;
import app.bpartners.api.repository.jpa.ProspectJpaRepository;
import app.bpartners.api.repository.jpa.model.HProspect;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitApi;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.SingleBuildingPermit;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.transaction.annotation.Isolation.SERIALIZABLE;

@AllArgsConstructor
@Repository

public class ProspectRepositoryImpl implements ProspectRepository {
  private final ProspectJpaRepository jpaRepository;
  private final ProspectMapper mapper;
  private final BuildingPermitApi buildingPermitApi;
  private final SogefiBuildingPermitRepository sogefiBuildingPermitRepository;

  //todo: an accountholder should get new prospects if he prospects from sogefi and if he needs
  // prospects
  @Override
  public List<Prospect> findAllByIdAccountHolder(String idAccountHolder) {
    boolean isSogefiProspector = true;
    if (isSogefiProspector) {
      buildingPermitApi.getData().getRecords()
          .forEach(
              buildingPermit -> {
                SingleBuildingPermit singleBuildingPermit =
                    buildingPermitApi.getOne(String.valueOf(buildingPermit.getFileId()));
                sogefiBuildingPermitRepository.saveByBuildingPermit(idAccountHolder,
                    buildingPermit, singleBuildingPermit);
              });
    }
    return jpaRepository.findAllByIdAccountHolder(idAccountHolder)
        .stream()
        .map(prospect -> mapper.toDomain(prospect, isSogefiProspector))
        .collect(Collectors.toUnmodifiableList());
  }

  @Transactional(isolation = SERIALIZABLE)
  @Override
  public List<Prospect> saveAll(List<Prospect> prospects) {
    boolean isSogefiProspector = true;
    List<HProspect> entities = prospects
        .stream()
        .map(mapper::toEntity)
        .collect(Collectors.toUnmodifiableList());
    return jpaRepository.saveAll(entities)
        .stream()
        .map(entity -> mapper.toDomain(entity, isSogefiProspector))
        .collect(Collectors.toUnmodifiableList());
  }
}
