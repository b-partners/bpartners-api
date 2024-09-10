package app.bpartners.api.repository.implementation;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.repository.expressif.fact.NewIntervention.OldCustomer.OldCustomerType.INDIVIDUAL;
import static org.springframework.transaction.annotation.Isolation.SERIALIZABLE;

import app.bpartners.api.endpoint.rest.model.ContactNature;
import app.bpartners.api.endpoint.rest.model.Geojson;
import app.bpartners.api.endpoint.rest.model.ProspectStatus;
import app.bpartners.api.endpoint.rest.security.AuthenticatedResourceProvider;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.AnnualRevenueTarget;
import app.bpartners.api.model.BusinessActivity;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.mapper.ProspectEvalMapper;
import app.bpartners.api.model.mapper.ProspectMapper;
import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.repository.AccountHolderRepository;
import app.bpartners.api.repository.ProspectRepository;
import app.bpartners.api.repository.SogefiBuildingPermitRepository;
import app.bpartners.api.repository.expressif.ExpressifApi;
import app.bpartners.api.repository.expressif.ProspectEval;
import app.bpartners.api.repository.expressif.ProspectResult;
import app.bpartners.api.repository.expressif.fact.NewIntervention;
import app.bpartners.api.repository.expressif.fact.Robbery;
import app.bpartners.api.repository.expressif.model.InputForm;
import app.bpartners.api.repository.expressif.model.InputValue;
import app.bpartners.api.repository.expressif.model.OutputValue;
import app.bpartners.api.repository.jpa.MunicipalityJpaRepository;
import app.bpartners.api.repository.jpa.ProspectEvalInfoJpaRepository;
import app.bpartners.api.repository.jpa.ProspectJpaRepository;
import app.bpartners.api.repository.jpa.model.HProspect;
import app.bpartners.api.repository.jpa.model.HProspectEval;
import app.bpartners.api.repository.jpa.model.HProspectEvalInfo;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitApi;
import app.bpartners.api.service.AnnualRevenueTargetService;
import app.bpartners.api.service.BusinessActivityService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apfloat.Aprational;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Repository
@Slf4j
public class ProspectRepositoryImpl implements ProspectRepository {
  public static final String TILE_LAYER = "carreleur";
  public static final String ANTI_HARM = "Antinuisibles 3D";
  public static final String ROOFER = "toiturier";
  public static final double UNPROCESSED_VALUE = -1.0;
  private final ProspectJpaRepository jpaRepository;
  private final ProspectMapper mapper;
  private final BuildingPermitApi buildingPermitApi;
  private final SogefiBuildingPermitRepository sogefiBuildingPermitRepository;
  private final BusinessActivityService businessActivityService;
  private final AuthenticatedResourceProvider resourceProvider;
  private final AnnualRevenueTargetService revenueTargetService;
  private final AccountHolderRepository accountHolderRepository;
  private final MunicipalityJpaRepository municipalityJpaRepository;
  private final ExpressifApi expressifApi;
  private final ProspectEvalMapper evalMapper;
  private final ProspectEvalInfoJpaRepository evalRepository;
  private final EntityManager em;
  private final SogefiBuildingPermitRepository sogefiRepository;

  @Override
  public Prospect getById(String id) {
    HProspect prospect = jpaRepository.findById(id).orElse(null);
    return prospect == null
        ? null
        : mapper.toDomain(
            prospect,
            prospect.getPosLatitude() == null && prospect.getPosLongitude() == null
                ? null
                : new Geojson()
                    .latitude(prospect.getPosLatitude())
                    .longitude(prospect.getPosLongitude()));
  }

  @Override
  public List<Prospect> findAllByStatus(ProspectStatus status) {
    return jpaRepository.findAllByStatus(status.getValue()).stream()
        .map(
            prospect ->
                mapper.toDomain(
                    prospect,
                    prospect.getPosLatitude() == null && prospect.getPosLongitude() == null
                        ? null
                        : new Geojson()
                            .latitude(prospect.getPosLatitude())
                            .longitude(prospect.getPosLongitude())))
        .collect(Collectors.toList());
  }

  @Override
  public List<Prospect> findAllByIdAccountHolder(
      String idAccountHolder,
      String name,
      ContactNature contactNature,
      ProspectStatus prospectStatus,
      int page,
      int pageSize) {
    BusinessActivity businessActivity =
        businessActivityService.findByAccountHolderId(idAccountHolder);
    boolean isSogefiProspector = isSogefiProspector(businessActivity);
    Pageable pageable = PageRequest.of(page, pageSize, Sort.by("lastEvaluationDate").descending());
    if (!isSogefiProspector) {
      List<HProspect> prospects;
      if (contactNature == null && prospectStatus == null) {
        prospects =
            jpaRepository.findAllByIdAccountHolderAndOldNameContainingIgnoreCase(
                idAccountHolder, name, pageable);
      } else if (prospectStatus != null && contactNature == null) {
        prospects =
            jpaRepository.findAllByIdAccountHolderAndOldNameAndProspectStatus(
                prospectStatus.toString(), idAccountHolder, name, pageSize, page);
        log.info("Prospect is not null={}", prospects);
      } else if (prospectStatus == null && contactNature != null) {
        prospects =
            jpaRepository.findAllByIdAccountHolderAndOldNameContainingIgnoreCaseAndContactNature(
                idAccountHolder, name, contactNature, pageable);
        log.info("Contact nature is not null={}", prospects);
      } else {
        prospects =
            jpaRepository
                .findAllByIdAccountHolderAndOldNameContainingIgnoreCaseAndContactNatureAndPropsectStatus(
                    idAccountHolder, name, contactNature.toString(), prospectStatus.toString(), pageSize, page);
        log.info("Here{}");
      }
      return prospects.stream()
          .map(
              prospect ->
                  mapper.toDomain(
                      prospect,
                      prospect.getPosLatitude() == null && prospect.getPosLongitude() == null
                          ? null
                          : new Geojson()
                              .latitude(prospect.getPosLatitude())
                              .longitude(prospect.getPosLongitude())))
          .sorted(Comparator.reverseOrder())
          .collect(Collectors.toList());
    }
    AccountHolder accountHolder = accountHolderRepository.findById(idAccountHolder);
    if (accountHolder.getTownCode() == null) {
      throw new BadRequestException(
          "AccountHolder.id="
              + idAccountHolder
              + " is missing the "
              + "required property town "
              + "code");
    }

    // TODO: Refactor municipality not use postgis extension
    //    List<HMunicipality> municipalities =
    //        municipalityJpaRepository.findMunicipalitiesWithinDistance(
    //            String.valueOf(accountHolder.getTownCode()),
    // accountHolder.getProspectingPerimeter());
    //    String townCodes =
    //        municipalities.stream()
    //            .map(HMunicipality::getCode)
    //            .collect(Collectors.joining(","));
    //    List<Integer> townCodesAsInt = municipalities.stream()
    //        .map(HMunicipality::getCode)
    //        .mapToInt(Integer::parseInt)
    //        .boxed()
    //        .collect(toUnmodifiableList());
    //    BuildingPermitList buildingPermitList =
    // buildingPermitApi.getBuildingPermitList(townCodes);
    //    if (buildingPermitList != null && buildingPermitList.getRecords() != null) {
    //      buildingPermitList.getRecords().forEach(buildingPermit -> {
    //        SingleBuildingPermit singleBuildingPermit =
    //
    // buildingPermitApi.getSingleBuildingPermit(String.valueOf(buildingPermit.getFileId()));
    //        sogefiBuildingPermitRepository.saveByBuildingPermit(idAccountHolder, buildingPermit,
    //            singleBuildingPermit);
    //      });
    //    }
    // TODO: why do prospects must be filtered by town code
    // while it is already attached to account holder ?
    return jpaRepository.findAllByIdAccountHolder(idAccountHolder, pageable).stream()
        .map(prospect -> toDomain(isSogefiProspector, prospect))
        .sorted(Comparator.reverseOrder())
        .collect(Collectors.toList());
  }

  public boolean isSogefiProspector(String idAccountHolder) {
    BusinessActivity businessActivity =
        businessActivityService.findByAccountHolderId(idAccountHolder);
    if (businessActivity == null) {
      return false;
    }
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
            && Objects.equals(
                0, TILE_LAYER.compareToIgnoreCase(businessActivity.getPrimaryActivity())))
        || (businessActivity.getSecondaryActivity() != null
            && Objects.equals(
                0, TILE_LAYER.compareToIgnoreCase(businessActivity.getSecondaryActivity())))
        || (businessActivity.getPrimaryActivity() != null
            && Objects.equals(0, ROOFER.compareToIgnoreCase(businessActivity.getPrimaryActivity())))
        || (businessActivity.getSecondaryActivity() != null
            && Objects.equals(
                0, ROOFER.compareToIgnoreCase(businessActivity.getSecondaryActivity())));
  }

  @Override
  public List<ProspectResult> evaluate(List<ProspectEval> toEvaluate) {
    List<HProspectEvalInfo> prospectEvalEntities = new ArrayList<>();
    for (ProspectEval prospectEval : toEvaluate) {
      Instant evaluationDate = Instant.now();

      List<InputValue> evalInputs = new ArrayList<>();
      convertEvalDefaultAttr(prospectEval, evaluationDate, evalInputs);
      convertEvalRuleAttr(prospectEval, evaluationDate, evalInputs);

      List<OutputValue> evalResult =
          expressifApi.process(
              InputForm.builder().evaluationDate(evaluationDate).inputValues(evalInputs).build());

      AtomicReference<Double> prospectRating = new AtomicReference<>(UNPROCESSED_VALUE);
      AtomicReference<Double> customerRating = new AtomicReference<>(UNPROCESSED_VALUE);
      if (evalResult.isEmpty()) {
        log.warn("[ExpressIF] Any result retrieved from " + evalInputs);
      } else {
        evalResult.forEach(
            result -> {
              if (result.getName().equals("Notation du prospect")) {
                prospectRating.set((Double) result.getValue());
              } else if (result.getName().equals("Notation de l'ancien client")) {
                customerRating.set((Double) result.getValue());
              }
            });
      }
      HProspectEval lastEval =
          evalMapper.toInfoEntity(
              prospectEval, evaluationDate, prospectRating.get(), customerRating.get());
      prospectEvalEntities.add(getInfoEntity(prospectEval, lastEval));
    }
    return evalRepository.saveAll(prospectEvalEntities).stream()
        .map(evalMapper::toResultDomain)
        .collect(Collectors.toList());
  }

  private HProspectEvalInfo getInfoEntity(ProspectEval prospectEval, HProspectEval lastEval) {
    HProspectEvalInfo entity =
        evalRepository
            .findById(prospectEval.getId())
            .orElse(
                evalMapper.toInfoEntity(prospectEval, getNextEvalReference(), new ArrayList<>()));

    entity.getProspectEvals().add(lastEval);
    return entity;
  }

  private void convertEvalRuleAttr(
      ProspectEval prospectEval, Instant evaluationDate, List<InputValue> evalInputs) {
    String prospectDepaRule = prospectEval.getDepaRule().getClass().getTypeName();
    if (prospectDepaRule.equals(NewIntervention.class.getTypeName())) {
      convertEvalNewInterventionAttr(prospectEval, evaluationDate, evalInputs);
    } else if (prospectDepaRule.equals(Robbery.class.getTypeName())) {
      convertEvalRobberyAttr(prospectEval, evaluationDate, evalInputs);
    } else {
      throw new ApiException(SERVER_EXCEPTION, "Unknown Depa rule applied " + prospectDepaRule);
    }
  }

  private void convertEvalRobberyAttr(
      ProspectEval prospectEval, Instant evaluationDate, List<InputValue> evalInputs) {
    Robbery depaRule = (Robbery) prospectEval.getDepaRule();
    if (depaRule.getDeclared() != null) {
      evalInputs.add(
          InputValue.builder()
              .evaluationDate(evaluationDate)
              .name("La déclaration de cambriolage")
              .value(depaRule.getDeclared())
              .build());
    }
    if (depaRule.getDistRobberyAndProspect() != null) {
      evalInputs.add(
          InputValue.builder()
              .evaluationDate(evaluationDate)
              .name("La distance entre un cambriolage et le prospect")
              .value(depaRule.getDistRobberyAndProspect())
              .build());
    }
    if (depaRule.getOldCustomer() != null) {
      evalInputs.add(
          InputValue.builder()
              .evaluationDate(evaluationDate)
              .name("La distance entre un cambriolage et l'ancien client")
              .value(depaRule.getOldCustomer().getDistRobberyAndOldCustomer())
              .build());
    }
  }

  private void convertEvalNewInterventionAttr(
      ProspectEval prospectEval, Instant evaluationDate, List<InputValue> evalInputs) {
    NewIntervention depaRule = (NewIntervention) prospectEval.getDepaRule();
    if (depaRule.getPlanned() != null) {
      evalInputs.add(
          InputValue.builder()
              .evaluationDate(evaluationDate)
              .name("Intervention prévue")
              .value(depaRule.getPlanned())
              .build());
    }
    if (depaRule.getInterventionType() != null) {
      evalInputs.add(
          InputValue.builder()
              .evaluationDate(evaluationDate)
              .name("Le type de l'intervention prévue contre les nuisibles")
              .value(depaRule.getInterventionType())
              .build());
    }
    if (depaRule.getInfestationType() != null) {
      evalInputs.add(
          InputValue.builder()
              .evaluationDate(evaluationDate)
              .name("Le type de l'infestation")
              .value(depaRule.getInfestationType())
              .build());
    }
    if (depaRule.getDistNewIntAndProspect() != null) {
      evalInputs.add(
          InputValue.builder()
              .evaluationDate(evaluationDate)
              .name("La distance entre l'intervention prévue et le prospect")
              .value(depaRule.getDistNewIntAndProspect())
              .build());
    }
    NewIntervention.OldCustomer oldCustomerFact = depaRule.getOldCustomer();
    if (oldCustomerFact.getProfessionalType() != null) {
      evalInputs.add(
          InputValue.builder()
              .evaluationDate(evaluationDate)
              .name("Le type de professionnel")
              .value(oldCustomerFact.getProfessionalType())
              .build());
    }
    if (oldCustomerFact.getType() != null) {
      evalInputs.add(
          InputValue.builder()
              .evaluationDate(evaluationDate)
              .name("Le type de client")
              .value(oldCustomerFact.getType() == INDIVIDUAL ? "particulier" : "professionnel")
              .build());
    }
    if (oldCustomerFact.getDistNewIntAndOldCustomer() != null) {
      evalInputs.add(
          InputValue.builder()
              .evaluationDate(evaluationDate)
              .name("La distance entre l'intervention prévue et l'ancien client")
              .value(oldCustomerFact.getDistNewIntAndOldCustomer())
              .build());
    }
  }

  private void convertEvalDefaultAttr(
      ProspectEval prospectEval, Instant evaluationDate, List<InputValue> evalInputs) {
    if (prospectEval.getLockSmith() != null) {
      evalInputs.add(
          InputValue.builder()
              .evaluationDate(evaluationDate)
              .name("Serrurier")
              .value(prospectEval.getLockSmith())
              .build());
    }
    if (prospectEval.getAntiHarm() != null) {
      evalInputs.add(
          InputValue.builder()
              .evaluationDate(evaluationDate)
              .name("Antinuisibles 3D")
              .value(prospectEval.getAntiHarm())
              .build());
    }
    if (prospectEval.getInsectControl() != null) {
      evalInputs.add(
          InputValue.builder()
              .evaluationDate(evaluationDate)
              .name("Service de désinsectisation")
              .value(prospectEval.getInsectControl())
              .build());
    }
    if (prospectEval.getDisinfection() != null) {
      evalInputs.add(
          InputValue.builder()
              .evaluationDate(evaluationDate)
              .name("Service de désinfection")
              .value(prospectEval.getDisinfection())
              .build());
    }
    if (prospectEval.getRatRemoval() != null) {
      evalInputs.add(
          InputValue.builder()
              .evaluationDate(evaluationDate)
              .name("Service de dératisation")
              .value(prospectEval.getRatRemoval())
              .build());
    }
    if (prospectEval.getProfessionalCustomer() != null) {
      evalInputs.add(
          InputValue.builder()
              .evaluationDate(evaluationDate)
              .name("Clientèle professionnelle")
              .value(prospectEval.getProfessionalCustomer())
              .build());
    }
    if (prospectEval.getParticularCustomer() != null) {
      evalInputs.add(
          InputValue.builder()
              .evaluationDate(evaluationDate)
              .name("Clientèle particulier")
              .value(prospectEval.getParticularCustomer())
              .build());
    }
  }

  @Transactional(isolation = SERIALIZABLE)
  @Override
  public List<Prospect> saveAll(List<Prospect> prospects) {
    AccountHolder authenticatedAccount = resourceProvider.getDefaultAccountHolder();
    boolean isSogefiProspector = isSogefiProspector(authenticatedAccount.getId());
    List<HProspect> entities =
        prospects.stream()
            .map(
                prospect -> {
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
    HProspect existing = jpaRepository.findById(prospect.getId()).orElse(null);
    HProspect entity = mapper.toEntity(prospect, existing);
    boolean isSogefiProspector = isSogefiProspector(prospect.getIdHolderOwner());
    return toDomain(isSogefiProspector, jpaRepository.save(entity));
  }

  private Prospect toDomain(boolean isSogefiProspector, HProspect entity) {
    Geojson domainGeojson =
        entity.getPosLatitude() == null && entity.getPosLongitude() == null
            ? null
            : new Geojson().latitude(entity.getPosLatitude()).longitude(entity.getPosLongitude());
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
  public List<Prospect> create(List<Prospect> prospects) {
    List<HProspect> toSave =
        prospects.stream()
            .map(
                prospect -> {
                  Prospect.ProspectRating prospectRating = prospect.getRating();
                  return mapper.toEntity(
                      prospect,
                      prospect.getIdHolderOwner(),
                      prospectRating.getValue(),
                      prospectRating.getLastEvaluationDate());
                })
            .collect(Collectors.toList());
    return jpaRepository.saveAll(toSave).stream()
        .map(
            prospect ->
                mapper.toDomain(
                    prospect,
                    prospect.getPosLatitude() == null && prospect.getPosLongitude() == null
                        ? null
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
        date == null
            ? new Fraction(BigInteger.valueOf(LocalDate.now().getDayOfYear()))
            : new Fraction(BigInteger.valueOf(date.getDayOfYear()));
    Fraction expectedAmountAttemptedToday =
        expectedAmountAttemptedPerDay.operate(todayAsFraction, Aprational::multiply);
    return revenueTargetsInAyear.get().getAmountAttempted().compareTo(expectedAmountAttemptedToday)
        == -1;
  }

  private Long getNextEvalReference() {
    Query query = em.createNativeQuery("select nextval('prospect_eval_info_ref_seq');");
    return ((Number) query.getSingleResult()).longValue();
  }
}
