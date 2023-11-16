package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.model.ContactNature;
import app.bpartners.api.endpoint.rest.model.Geojson;
import app.bpartners.api.endpoint.rest.model.ProspectStatus;
import app.bpartners.api.endpoint.rest.security.AuthenticatedResourceProvider;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.AnnualRevenueTarget;
import app.bpartners.api.model.BusinessActivity;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.mapper.ProspectMapper;
import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.repository.AccountHolderRepository;
import app.bpartners.api.repository.ProspectRepository;
import app.bpartners.api.repository.SogefiBuildingPermitRepository;
import app.bpartners.api.repository.jpa.MunicipalityJpaRepository;
import app.bpartners.api.repository.jpa.ProspectJpaRepository;
import app.bpartners.api.repository.jpa.model.HProspect;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitApi;
import app.bpartners.api.service.AnnualRevenueTargetService;
import app.bpartners.api.service.BusinessActivityService;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.Year;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apfloat.Aprational;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.transaction.annotation.Isolation.SERIALIZABLE;

@AllArgsConstructor
@Repository
@Slf4j
public class ProspectRepositoryImpl implements ProspectRepository {
  public static final String TILE_LAYER = "carreleur";
  public static final String ANTI_HARM = "Antinuisibles 3D";
  public static final String ROOFER = "toiturier";
  private final ProspectJpaRepository jpaRepository;
  private final ProspectMapper mapper;
  private final BuildingPermitApi buildingPermitApi;
  private final SogefiBuildingPermitRepository sogefiBuildingPermitRepository;
  private final BusinessActivityService businessActivityService;
  private final AuthenticatedResourceProvider resourceProvider;
  private final AnnualRevenueTargetService revenueTargetService;
  private final AccountHolderRepository accountHolderRepository;
  private final MunicipalityJpaRepository municipalityJpaRepository;
  private final SogefiBuildingPermitRepository sogefiRepository;

  @Override
  public Prospect getById(String id) {
    HProspect prospect = jpaRepository.findById(id).orElse(null);
    return prospect == null ? null : mapper.toDomain(prospect,
        prospect.getPosLatitude() == null && prospect.getPosLongitude() == null ? null
            : new Geojson()
            .latitude(prospect.getPosLatitude())
            .longitude(prospect.getPosLongitude()));
  }

  @Override
  public List<Prospect> findAllByStatus(ProspectStatus status) {
    return jpaRepository.findAllByStatus(status.getValue()).stream()
        .map(prospect -> mapper.toDomain(prospect,
            prospect.getPosLatitude() == null && prospect.getPosLongitude() == null ? null
                : new Geojson()
                .latitude(prospect.getPosLatitude())
                .longitude(prospect.getPosLongitude())))
        .collect(Collectors.toList());
  }

  @Override
  public List<Prospect> findAllByIdAccountHolder(String idAccountHolder,
                                                 String name,
                                                 ContactNature contactNature) {
    BusinessActivity businessActivity =
        businessActivityService.findByAccountHolderId(idAccountHolder);
    boolean isSogefiProspector = isSogefiProspector(businessActivity);
    if (!isSogefiProspector) {
      List<HProspect> prospects = contactNature == null
          ? jpaRepository.findAllByIdAccountHolderAndOldNameContainingIgnoreCase(
          idAccountHolder,
          name)
          : jpaRepository.findAllByIdAccountHolderAndOldNameContainingIgnoreCaseAndContactNature(
          idAccountHolder,
          name,
          contactNature);
      return prospects.stream()
          .map(prospect -> mapper.toDomain(prospect,
              prospect.getPosLatitude() == null && prospect.getPosLongitude() == null ? null
                  : new Geojson()
                  .latitude(prospect.getPosLatitude())
                  .longitude(prospect.getPosLongitude())))
          .sorted(Comparator.reverseOrder())
          .collect(Collectors.toList());
    }
    AccountHolder accountHolder = accountHolderRepository.findById(idAccountHolder);
    if (accountHolder.getTownCode() == null) {
      throw new BadRequestException(
          "AccountHolder.id=" + idAccountHolder + " is missing the " + "required property town "
              + "code");
    }

    //TODO: Refactor municipality not use postgis extension
//    List<HMunicipality> municipalities =
//        municipalityJpaRepository.findMunicipalitiesWithinDistance(
//            String.valueOf(accountHolder.getTownCode()), accountHolder.getProspectingPerimeter());
//    String townCodes =
//        municipalities.stream()
//            .map(HMunicipality::getCode)
//            .collect(Collectors.joining(","));
//    List<Integer> townCodesAsInt = municipalities.stream()
//        .map(HMunicipality::getCode)
//        .mapToInt(Integer::parseInt)
//        .boxed()
//        .collect(toUnmodifiableList());
//    BuildingPermitList buildingPermitList = buildingPermitApi.getBuildingPermitList(townCodes);
//    if (buildingPermitList != null && buildingPermitList.getRecords() != null) {
//      buildingPermitList.getRecords().forEach(buildingPermit -> {
//        SingleBuildingPermit singleBuildingPermit =
//            buildingPermitApi.getSingleBuildingPermit(String.valueOf(buildingPermit.getFileId()));
//        sogefiBuildingPermitRepository.saveByBuildingPermit(idAccountHolder, buildingPermit,
//            singleBuildingPermit);
//      });
//    }
    //TODO: why do prospects must be filtered by town code
    // while it is already attached to account holder ?
    return jpaRepository.findAllByIdAccountHolder(idAccountHolder)
        .stream()
        .map(prospect -> toDomain(isSogefiProspector, prospect))
        .sorted(Comparator.reverseOrder())
        .collect(Collectors.toList());
  }

  public boolean isSogefiProspector(String idAccountHolder) {
    BusinessActivity businessActivity =
        businessActivityService.findByAccountHolderId(idAccountHolder);
    String secondaryActivity = businessActivity.getSecondaryActivity();
    String primaryActivity = businessActivity.getPrimaryActivity();
    if (primaryActivity == null && secondaryActivity == null) {
      return false;
    }
    return primaryActivity != null
        && Objects.equals(0, TILE_LAYER.compareToIgnoreCase(primaryActivity))
        || secondaryActivity != null
        && Objects.equals(0, TILE_LAYER.compareToIgnoreCase(secondaryActivity))
        || primaryActivity != null && Objects.equals(0, ROOFER.compareToIgnoreCase(primaryActivity))
        || secondaryActivity != null
        && Objects.equals(0, ROOFER.compareToIgnoreCase(secondaryActivity));
  }

  public boolean isSogefiProspector(BusinessActivity businessActivity) {
    return (businessActivity.getPrimaryActivity() != null
        && Objects.equals(0, TILE_LAYER.compareToIgnoreCase(businessActivity.getPrimaryActivity())))
        || (businessActivity.getSecondaryActivity() != null
        && Objects.equals(0,
        TILE_LAYER.compareToIgnoreCase(businessActivity.getSecondaryActivity())))
        || (businessActivity.getPrimaryActivity() != null
        && Objects.equals(
        0, ROOFER.compareToIgnoreCase(businessActivity.getPrimaryActivity())))
        || (businessActivity.getSecondaryActivity() != null
        && Objects.equals(0,
        ROOFER.compareToIgnoreCase(businessActivity.getSecondaryActivity())));
  }

  @Transactional(isolation = SERIALIZABLE)
  @Override
  public List<Prospect> saveAll(List<Prospect> prospects) {
    AccountHolder authenticatedAccount = resourceProvider.getDefaultAccountHolder();
    boolean isSogefiProspector = isSogefiProspector(authenticatedAccount.getId());
    List<HProspect> entities =
        prospects.stream()
            .map(prospect -> {
              Optional<HProspect> optionalProspect = jpaRepository.findById(prospect.getId());
              HProspect existing = optionalProspect.orElse(null);
              return mapper.toEntity(prospect, existing);
            })
            .toList();
    return jpaRepository.saveAll(entities).stream()
        .map(entity -> toDomain(isSogefiProspector, entity))
        .toList();
  }

  @Override
  public Prospect save(Prospect prospect) {
    HProspect existing = jpaRepository.findById(prospect.getId())
        .orElse(null);
    HProspect entity = mapper.toEntity(prospect, existing);
    boolean isSogefiProspector = isSogefiProspector(prospect.getIdHolderOwner());
    return toDomain(isSogefiProspector, jpaRepository.save(entity));
  }

  private Prospect toDomain(boolean isSogefiProspector, HProspect entity) {
    Geojson domainGeojson =
        entity.getPosLatitude() == null && entity.getPosLongitude() == null ? null
            : new Geojson()
            .latitude(entity.getPosLatitude())
            .longitude(entity.getPosLongitude());
    Geojson location = domainGeojson;
    if (isSogefiProspector) {
      location = sogefiRepository.findLocationByIdProspect(entity.getId());
      if (location == null) {
        location = domainGeojson;
        log.warn("Prospect." + entity.getId() + " not found in prospecting database.");
      }
    }
    return mapper.toDomain(entity, location);
  }

  @Override
  public List<Prospect> createAll(List<Prospect> prospects) {
    List<HProspect> toSave = prospects.stream()
        .map(prospect -> {
          Prospect.ProspectRating prospectRating = prospect.getRating();
          return mapper.toEntity(
              prospect,
              prospect.getIdHolderOwner(),
              prospectRating.getValue(),
              prospectRating.getLastEvaluationDate());
        })
        .collect(Collectors.toList());
    return jpaRepository.saveAll(toSave).stream()
        .map(prospect -> mapper.toDomain(prospect,
            prospect.getPosLatitude() == null && prospect.getPosLongitude() == null ? null
                : new Geojson()
                .latitude(prospect.getPosLatitude())
                .longitude(prospect.getPosLongitude())))
        .collect(Collectors.toList());
  }

  @Override
  public boolean needsProspects(String idAccountHolder, LocalDate date) {
    Optional<AnnualRevenueTarget> revenueTargetsInAyear =
        revenueTargetService.getByYear(idAccountHolder, Year.now().getValue());

    if (revenueTargetsInAyear.isEmpty()) {
      return false;
    }

    Fraction year = new Fraction(BigInteger.valueOf(365));
    Fraction expectedAmountAttemptedPerDay =
        revenueTargetsInAyear.get().getAmountTarget().operate(year, Aprational::divide);
    Fraction todayAsFraction =
        date == null ? new Fraction(BigInteger.valueOf(LocalDate.now().getDayOfYear())) :
            new Fraction(BigInteger.valueOf(date.getDayOfYear()));
    Fraction expectedAmountAttemptedToday =
        expectedAmountAttemptedPerDay.operate(todayAsFraction, Aprational::multiply);
    return revenueTargetsInAyear.get().getAmountAttempted().compareTo(expectedAmountAttemptedToday)
        == -1;
  }
}
