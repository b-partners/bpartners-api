package app.bpartners.api.repository;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.mapper.ProspectEvalMapper;
import app.bpartners.api.model.mapper.ProspectEvaluationMapper;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.expressif.ExpressifApi;
import app.bpartners.api.repository.expressif.ProspectEvaluation;
import app.bpartners.api.repository.expressif.ProspectResult;
import app.bpartners.api.repository.expressif.fact.NewIntervention;
import app.bpartners.api.repository.expressif.fact.Robbery;
import app.bpartners.api.repository.expressif.model.InputForm;
import app.bpartners.api.repository.expressif.model.InputValue;
import app.bpartners.api.repository.expressif.model.OutputValue;
import app.bpartners.api.repository.google.sheets.SheetApi;
import app.bpartners.api.repository.jpa.ProspectEvaluationInfoJpaRepository;
import app.bpartners.api.repository.jpa.model.HProspectEvaluation;
import app.bpartners.api.repository.jpa.model.HProspectEvaluationInfo;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.repository.expressif.fact.NewIntervention.OldCustomer.OldCustomerType.INDIVIDUAL;
import static app.bpartners.api.repository.google.sheets.SheetConf.GRID_SHEET_TYPE;

@Repository
@AllArgsConstructor
@Slf4j
public class ProspectEvaluationRepositoryImpl
    implements ProspectEvaluationRepository {
  public static final double UNPROCESSED_VALUE = -1.0;
  private final ExpressifApi expressifApi;
  private final SheetApi sheetApi;
  private final BanApi banApi;
  private final ProspectEvalMapper evalMapper;
  private final ProspectEvaluationMapper evaluationMapper;
  private final ProspectEvaluationInfoJpaRepository infoRepository;
  private final EntityManager em;

  @Override
  public List<ProspectEvaluation> findBySpreadsheet(String spreadsheetName, String sheetName,
                                                    Integer minRange, Integer maxRange) {
    Spreadsheet spreadsheet =
        sheetApi.getSpreadsheetByNames(spreadsheetName, sheetName, minRange, maxRange);
    List<Sheet> sheets = spreadsheet.getSheets();
    String sheetNameNotFoundMsg = "Sheet(name=" + sheetName + ")"
        + " inside Spreadsheet(name=" + spreadsheet.getProperties().getTitle()
        + ") does not exist";
    if (sheets.isEmpty()) {
      throw new BadRequestException("Spreadsheet has empty sheets or " + sheetNameNotFoundMsg);
    }
    Sheet sheet = sheets.stream()
        .filter(s -> s.getProperties().getTitle().equals(sheetName))
        .findAny().orElseThrow(
            () -> new NotFoundException(sheetNameNotFoundMsg));
    if (!sheet.getProperties().getSheetType().equals(GRID_SHEET_TYPE)) {
      throw new NotImplementedException("Only GRID sheet type is supported");
    }
    return evaluationMapper.toProspectEvaluation(sheet, banApi);
  }


  @Override
  public List<ProspectResult> evaluate(List<ProspectEvaluation> toEvaluate) {
    List<HProspectEvaluationInfo> prospectEvalEntities = new ArrayList<>();
    for (ProspectEvaluation prospectEvaluation : toEvaluate) {
      Instant evaluationDate = Instant.now();

      List<InputValue> evalInputs = new ArrayList<>();
      convertEvaluationDefaultAttributes(prospectEvaluation, evaluationDate, evalInputs);
      convertEvaluationRuleAttributes(prospectEvaluation, evaluationDate, evalInputs);

      List<OutputValue> evalResult = expressifApi.process(InputForm.builder()
          .evaluationDate(evaluationDate)
          .inputValues(evalInputs)
          .build());

      AtomicReference<Double> prospectRating = new AtomicReference<>(UNPROCESSED_VALUE);
      AtomicReference<Double> customerRating = new AtomicReference<>(UNPROCESSED_VALUE);
      if (evalResult.isEmpty()) {
        log.warn("[ExpressIF] Any result retrieved from " + evalInputs);
      } else {
        evalResult.forEach(result -> {
          if (result.getName().equals("Notation du prospect")) {
            prospectRating.set((Double) result.getValue());
          } else if (result.getName().equals("Notation de l'ancien client")) {
            customerRating.set((Double) result.getValue());
          }
        });
      }
      HProspectEvaluation lastEval =
          evalMapper.toInfoEntity(prospectEvaluation, evaluationDate,
              prospectRating.get(), customerRating.get());
      prospectEvalEntities.add(getEvaluationInfoEntity(prospectEvaluation, lastEval));
    }
    return infoRepository.saveAll(prospectEvalEntities).stream()
        .map(evalMapper::toResultDomain)
        .collect(Collectors.toList());
  }

  private HProspectEvaluationInfo getEvaluationInfoEntity(ProspectEvaluation prospectEvaluation,
                                                          HProspectEvaluation lastEval) {
    HProspectEvaluationInfo entity = infoRepository.findById(prospectEvaluation.getId())
        .orElse(
            evalMapper.toInfoEntity(prospectEvaluation, nextInfoReference(), new ArrayList<>()));

    entity.getProspectEvals().add(lastEval);
    return entity;
  }

  private void convertEvaluationRuleAttributes(
      ProspectEvaluation prospectEvaluation, Instant evaluationDate, List<InputValue> evalInputs) {
    String prospectDepaRule = prospectEvaluation.getDepaRule().getClass().getTypeName();
    if (prospectDepaRule.equals(NewIntervention.class.getTypeName())) {
      convertEvaluationNewInterventionAttributes(prospectEvaluation, evaluationDate, evalInputs);
    } else if (prospectDepaRule.equals(Robbery.class.getTypeName())) {
      convertEvaluationRobberyAttributes(prospectEvaluation, evaluationDate, evalInputs);
    } else {
      throw new ApiException(SERVER_EXCEPTION, "Unknown Depa rule applied " + prospectDepaRule);
    }
  }

  private void convertEvaluationRobberyAttributes(
      ProspectEvaluation prospectEvaluation, Instant evaluationDate, List<InputValue> evalInputs) {
    Robbery depaRule = (Robbery) prospectEvaluation.getDepaRule();
    if (depaRule.getDeclared() != null) {
      evalInputs.add(InputValue.builder()
          .evaluationDate(evaluationDate)
          .name("La déclaration de cambriolage")
          .value(depaRule.getDeclared())
          .build());
    }
    if (depaRule.getDistRobberyAndProspect() != null) {
      evalInputs.add(InputValue.builder()
          .evaluationDate(evaluationDate)
          .name("La distance entre un cambriolage et le prospect")
          .value(depaRule.getDistRobberyAndProspect())
          .build());
    }
    if (depaRule.getOldCustomer() != null) {
      evalInputs.add(InputValue.builder()
          .evaluationDate(evaluationDate)
          .name("La distance entre un cambriolage et l'ancien client")
          .value(depaRule.getOldCustomer().getDistRobberyAndOldCustomer())
          .build());
    }
  }

  private void convertEvaluationNewInterventionAttributes(
      ProspectEvaluation prospectEvaluation, Instant evaluationDate, List<InputValue> evalInputs) {
    NewIntervention depaRule = (NewIntervention) prospectEvaluation.getDepaRule();
    if (depaRule.getPlanned() != null) {
      evalInputs.add(InputValue.builder()
          .evaluationDate(evaluationDate)
          .name("Intervention prévue")
          .value(depaRule.getPlanned())
          .build());
    }
    if (depaRule.getInterventionType() != null) {
      evalInputs.add(InputValue.builder()
          .evaluationDate(evaluationDate)
          .name("Le type de l'intervention prévue contre les nuisibles")
          .value(depaRule.getInterventionType())
          .build());
    }
    if (depaRule.getInfestationType() != null) {
      evalInputs.add(InputValue.builder()
          .evaluationDate(evaluationDate)
          .name("Le type de l'infestation")
          .value(depaRule.getInfestationType())
          .build());
    }
    if (depaRule.getDistNewIntAndProspect() != null) {
      evalInputs.add(InputValue.builder()
          .evaluationDate(evaluationDate)
          .name("La distance entre l'intervention prévue et le prospect")
          .value(depaRule.getDistNewIntAndProspect())
          .build());
    }
    NewIntervention.OldCustomer oldCustomerFact = depaRule.getOldCustomer();
    if (oldCustomerFact.getProfessionalType() != null) {
      evalInputs.add(InputValue.builder()
          .evaluationDate(evaluationDate)
          .name("Le type de professionnel")
          .value(oldCustomerFact.getProfessionalType())
          .build());
    }
    if (oldCustomerFact.getType() != null) {
      evalInputs.add(InputValue.builder()
          .evaluationDate(evaluationDate)
          .name("Le type de client")
          .value(oldCustomerFact.getType() == INDIVIDUAL
              ? "particulier"
              : "professionnel")
          .build());
    }
    if (oldCustomerFact.getDistNewIntAndOldCustomer() != null) {
      evalInputs.add(InputValue.builder()
          .evaluationDate(evaluationDate)
          .name("La distance entre l'intervention prévue et l'ancien client")
          .value(oldCustomerFact.getDistNewIntAndOldCustomer())
          .build());
    }
  }

  private void convertEvaluationDefaultAttributes(
      ProspectEvaluation prospectEvaluation, Instant evaluationDate, List<InputValue> evalInputs) {
    if (prospectEvaluation.getLockSmith() != null) {
      evalInputs.add(InputValue.builder()
          .evaluationDate(evaluationDate)
          .name("Serrurier")
          .value(prospectEvaluation.getLockSmith())
          .build());
    }
    if (prospectEvaluation.getAntiHarm() != null) {
      evalInputs.add(InputValue.builder()
          .evaluationDate(evaluationDate)
          .name("Antinuisibles 3D")
          .value(prospectEvaluation.getAntiHarm())
          .build());
    }
    if (prospectEvaluation.getInsectControl() != null) {
      evalInputs.add(InputValue.builder()
          .evaluationDate(evaluationDate)
          .name("Service de désinsectisation")
          .value(prospectEvaluation.getInsectControl())
          .build());
    }
    if (prospectEvaluation.getDisinfection() != null) {
      evalInputs.add(InputValue.builder()
          .evaluationDate(evaluationDate)
          .name("Service de désinfection")
          .value(prospectEvaluation.getDisinfection())
          .build());
    }
    if (prospectEvaluation.getRatRemoval() != null) {
      evalInputs.add(InputValue.builder()
          .evaluationDate(evaluationDate)
          .name("Service de dératisation")
          .value(prospectEvaluation.getRatRemoval())
          .build());
    }
    if (prospectEvaluation.getProfessionalCustomer() != null) {
      evalInputs.add(InputValue.builder()
          .evaluationDate(evaluationDate)
          .name("Clientèle professionnelle")
          .value(prospectEvaluation.getProfessionalCustomer())
          .build());
    }
    if (prospectEvaluation.getParticularCustomer() != null) {
      evalInputs.add(InputValue.builder()
          .evaluationDate(evaluationDate)
          .name("Clientèle particulier")
          .value(prospectEvaluation.getParticularCustomer())
          .build());
    }
  }

  private Long nextInfoReference() {
    Query query = em.createNativeQuery("select nextval('prospect_eval_info_ref_seq');");
    return ((Number) query.getSingleResult()).longValue();
  }
}
