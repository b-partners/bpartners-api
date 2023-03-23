package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.security.AuthenticatedResourceProvider;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.BusinessActivity;
import app.bpartners.api.model.Prospect;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.mapper.ProspectMapper;
import app.bpartners.api.repository.ProspectRepository;
import app.bpartners.api.repository.SogefiBuildingPermitRepository;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.ProspectJpaRepository;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import app.bpartners.api.repository.jpa.model.HProspect;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitApi;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.SingleBuildingPermit;
import app.bpartners.api.service.BusinessActivityService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.transaction.annotation.Isolation.SERIALIZABLE;

@AllArgsConstructor
@Repository

public class ProspectRepositoryImpl implements ProspectRepository {
  public static final String TILE_LAYER = "carreleur";
  public static final String ROOFER = "toiturier";
  private final ProspectJpaRepository jpaRepository;
  private final ProspectMapper mapper;
  private final BuildingPermitApi buildingPermitApi;
  private final SogefiBuildingPermitRepository sogefiBuildingPermitRepository;
  private final BusinessActivityService businessActivityService;
  private final AuthenticatedResourceProvider resourceProvider;
  private final AccountHolderJpaRepository accountHolderJpaRepository;

  @Override
  public List<Prospect> findAllByIdAccountHolder(String idAccountHolder) {
    boolean isSogefiProspector = isSogefiProspector(idAccountHolder);
    Optional<HAccountHolder> optionalAccountHolderEntity =
        accountHolderJpaRepository.findById(idAccountHolder);
    if (optionalAccountHolderEntity.isEmpty()
        || optionalAccountHolderEntity.get().getTownCode() == null) {
      throw new BadRequestException(
          "AccountHolder." + idAccountHolder + " is missing the " + "required property town code");
    }
    String townCode = String.valueOf(optionalAccountHolderEntity.get().getTownCode());
    if (isSogefiProspector) {
      buildingPermitApi.getData(townCode).getRecords().forEach(buildingPermit -> {
        SingleBuildingPermit singleBuildingPermit =
            buildingPermitApi.getOne(String.valueOf(buildingPermit.getFileId()));
        sogefiBuildingPermitRepository.saveByBuildingPermit(idAccountHolder, buildingPermit,
            singleBuildingPermit);
      });
    }
    return jpaRepository.findAllByIdAccountHolder(idAccountHolder).stream()
        .map(prospect -> mapper.toDomain(prospect, isSogefiProspector))
        .collect(Collectors.toUnmodifiableList());
  }

  private boolean isSogefiProspector(String idAccountHolder) {
    BusinessActivity businessActivity =
        businessActivityService.findByAccountHolderId(idAccountHolder);
    return Objects.equals(0, TILE_LAYER.compareToIgnoreCase(businessActivity.getPrimaryActivity()))
        || Objects.equals(0,
        TILE_LAYER.compareToIgnoreCase(businessActivity.getSecondaryActivity())) || Objects.equals(
        0, ROOFER.compareToIgnoreCase(businessActivity.getPrimaryActivity())) || Objects.equals(0,
        ROOFER.compareToIgnoreCase(businessActivity.getSecondaryActivity()));
  }

  @Transactional(isolation = SERIALIZABLE)
  @Override
  public List<Prospect> saveAll(List<Prospect> prospects) {
    AccountHolder authenticatedAccount = resourceProvider.getAccountHolder();

    boolean isSogefiProspector = isSogefiProspector(authenticatedAccount.getId());
    List<HProspect> entities =
        prospects.stream().map(mapper::toEntity).collect(Collectors.toUnmodifiableList());
    return jpaRepository.saveAll(entities).stream()
        .map(entity -> mapper.toDomain(entity, isSogefiProspector))
        .collect(Collectors.toUnmodifiableList());
  }
}
