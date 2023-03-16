package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.security.AuthenticatedResourceProvider;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.AnnualRevenueTarget;
import app.bpartners.api.model.BusinessActivity;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Prospect;
import app.bpartners.api.model.TransactionsSummary;
import app.bpartners.api.model.mapper.ProspectMapper;
import app.bpartners.api.repository.ProspectRepository;
import app.bpartners.api.repository.SogefiBuildingPermitRepository;
import app.bpartners.api.repository.jpa.ProspectJpaRepository;
import app.bpartners.api.repository.jpa.model.HProspect;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitApi;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.SingleBuildingPermit;
import app.bpartners.api.service.AccountHolderService;
import app.bpartners.api.service.AnnualRevenueTargetService;
import app.bpartners.api.service.BusinessActivityService;
import app.bpartners.api.service.ProspectService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
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
  private final AccountHolderService accountHolder;
  private final AnnualRevenueTargetService revenueTargetService;
  private  final TransactionsSummaryRepositoryImpl transactionsSummaryRepository;
  private final ProspectService prospectService;
  @Override
  public List<Prospect> findAllByIdAccountHolder(String idAccountHolder) {
    boolean isSogefiProspector = isSogefiProspector(idAccountHolder);
    if (isSogefiProspector) {
      buildingPermitApi.getData().getRecords().forEach(buildingPermit -> {
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
        prospects.stream().map(mapper::toEntity).collect(Collectors.toUnmodifiableList());
    return jpaRepository.saveAll(entities).stream()
        .map(entity -> mapper.toDomain(entity, isSogefiProspector))
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public boolean needsProspects(String idAccountHolder) {
    Optional<AnnualRevenueTarget> revenueTargetsInAYear =
        revenueTargetService.getByYear(idAccountHolder, Year.now().getValue());

    if (revenueTargetsInAYear.isEmpty()) {
      return false;
    }

    Instant instant = revenueTargetsInAYear.get().getUpdatedAt();
    LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    int dayNumber = localDateTime.getDayOfYear();

    Fraction expectedAmountAttemptedAtCurrentDay =
        parseFraction(revenueTargetsInAYear.get().getAmountTarget().getCentsRoundUp() * dayNumber / 365);
    Fraction amountAttemptedActual =
        parseFraction(revenueTargetsInAYear.get().getAmountAttempted().getCentsRoundUp());


    if(amountAttemptedActual.getCentsRoundUp() > expectedAmountAttemptedAtCurrentDay.getCentsRoundUp()){
      int extraMoney =
          (revenueTargetsInAYear.get().getAmountAttempted().getCentsRoundUp() * 100 / 10000) -
              (expectedAmountAttemptedAtCurrentDay.getCentsRoundUp() / 10000);
      int day =
          (extraMoney * 365 / revenueTargetsInAYear.get().getAmountTarget().getCentsRoundUp()) / 100;
      LocalDateTime dateTime = LocalDateTime.now().plusDays(day);
      prospectService.setEnabled(false);
      prospectService.setSilentUntil(dateTime);
    }

    return amountAttemptedActual.getCentsRoundUp() <
        expectedAmountAttemptedAtCurrentDay.getCentsRoundUp();
  }
}
