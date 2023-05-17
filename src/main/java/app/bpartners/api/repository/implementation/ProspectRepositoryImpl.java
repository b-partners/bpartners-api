package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.security.AuthenticatedResourceProvider;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.AnnualRevenueTarget;
import app.bpartners.api.model.BusinessActivity;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Prospect;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.mapper.ProspectMapper;
import app.bpartners.api.repository.AccountHolderRepository;
import app.bpartners.api.repository.ProspectRepository;
import app.bpartners.api.repository.SogefiBuildingPermitRepository;
import app.bpartners.api.repository.jpa.MunicipalityJpaRepository;
import app.bpartners.api.repository.jpa.ProspectJpaRepository;
import app.bpartners.api.repository.jpa.model.HMunicipality;
import app.bpartners.api.repository.jpa.model.HProspect;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitApi;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.BuildingPermitList;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.SingleBuildingPermit;
import app.bpartners.api.service.AnnualRevenueTargetService;
import app.bpartners.api.service.BusinessActivityService;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.apfloat.Aprational;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import static java.util.stream.Collectors.toUnmodifiableList;
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
  private final AnnualRevenueTargetService revenueTargetService;
  private final AccountHolderRepository accountHolderRepository;
  private final MunicipalityJpaRepository municipalityJpaRepository;

  @Override
  public List<Prospect> findAllByIdAccountHolder(String idAccountHolder) {
    boolean isSogefiProspector = isSogefiProspector(idAccountHolder);
    AccountHolder accountHolder = accountHolderRepository.findById(idAccountHolder);
    if (accountHolder.getTownCode() == null) {
      throw new BadRequestException(
          "AccountHolder.id=" + idAccountHolder + " is missing the " + "required property town "
              + "code");
    }
    List<HMunicipality> municipalities =
        municipalityJpaRepository.findMunicipalitiesWithinDistance(
            String.valueOf(accountHolder.getTownCode()), accountHolder.getProspectingPerimeter());
    String townCodes =
        municipalities.stream()
            .map(HMunicipality::getCode)
            .collect(Collectors.joining(","));
    List<Integer> townCodesAsInt = municipalities.stream()
        .map(HMunicipality::getCode)
        .mapToInt(Integer::parseInt)
        .boxed()
        .collect(toUnmodifiableList());
    if (isSogefiProspector) {
      BuildingPermitList buildingPermitList = buildingPermitApi.getBuildingPermitList(townCodes);
      buildingPermitList.getRecords().forEach(buildingPermit -> {
        SingleBuildingPermit singleBuildingPermit =
            buildingPermitApi.getSingleBuildingPermit(String.valueOf(buildingPermit.getFileId()));
        sogefiBuildingPermitRepository.saveByBuildingPermit(idAccountHolder, buildingPermit,
            singleBuildingPermit);
      });
    }
    return jpaRepository
        .findAllByIdAccountHolderAndTownCodeIsIn(idAccountHolder, townCodesAsInt).stream()
        .map(prospect -> mapper.toDomain(prospect, isSogefiProspector))
        .collect(toUnmodifiableList());
  }

  public boolean isSogefiProspector(String idAccountHolder) {
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
        prospects.stream().map(mapper::toEntity).collect(toUnmodifiableList());
    return jpaRepository.saveAll(entities).stream()
        .map(entity -> mapper.toDomain(entity, isSogefiProspector))
        .collect(toUnmodifiableList());
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
