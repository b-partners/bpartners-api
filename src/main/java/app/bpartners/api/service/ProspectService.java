package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.model.gen.ProspectUpdated;
import app.bpartners.api.endpoint.rest.model.ContactNature;
import app.bpartners.api.endpoint.rest.model.ProspectStatus;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.ProspectMapper;
import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.repository.ProspectRepository;
import app.bpartners.api.repository.expressif.ProspectEvaluation;
import app.bpartners.api.repository.expressif.ProspectEvaluationInfo;
import app.bpartners.api.service.aws.ProspectUpdatedService;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static app.bpartners.api.endpoint.rest.model.ProspectStatus.CONTACTED;
import static app.bpartners.api.endpoint.rest.model.ProspectStatus.CONVERTED;
import static app.bpartners.api.endpoint.rest.model.ProspectStatus.TO_CONTACT;

@Service
@AllArgsConstructor
@Slf4j
public class ProspectService {
  public static final int DEFAULT_RATING_PROSPECT_TO_CONVERT = 8;
  private final ProspectRepository repository;
  private final ProspectEvaluationService evaluationService;
  private final ProspectUpdatedService prospectUpdatedService;
  private final ProspectMapper prospectMapper;

  @Transactional
  public Prospect getById(String id) {
    return repository.getById(id);
  }

  @Transactional
  public List<Prospect> findAllByStatus(ProspectStatus prospectStatus) {
    return repository.findAllByStatus(prospectStatus);
  }

  @Transactional
  public List<Prospect> getByCriteria(String idAccountHolder,
                                      String name,
                                      String contactNatureValue) {
    ContactNature contactNature;
    try {
      contactNature = contactNatureValue == null ? null
          : ContactNature.valueOf(contactNatureValue);
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("Unknown contactNature type = " + contactNatureValue);
    }
    String nameValue = name == null ? "" : name;
    return repository.findAllByIdAccountHolder(idAccountHolder, nameValue, contactNature);
  }

  @Transactional
  public List<Prospect> saveAll(List<Prospect> toCreate) {
    return repository.saveAll(toCreate);
  }

  @Transactional
  public Prospect update(Prospect toSave) {
    Prospect existing = repository.getById(toSave.getId());
    if (existing == null) {
      throw new NotFoundException("Prospect(id=" + toSave.getId() + ") not found");
    }
    //validateStatusUpdateFlow(toSave, existing);
    Prospect savedProspect = repository.save(toSave);

    prospectUpdatedService.accept(ProspectUpdated.builder()
        .prospect(savedProspect)
        .updatedAt(Instant.now())
        .build());

    return savedProspect;
  }

  @Transactional
  public List<Prospect> importProspectsFromSpreadsheet(String spreadsheetName,
                                                       String sheetName,
                                                       Integer minRange,
                                                       Integer maxRange) {
    List<ProspectEvaluation> prospectEvaluations = evaluationService.readEvaluations(
        spreadsheetName,
        sheetName,
        minRange,
        maxRange);

    List<Prospect> prospects = prospectEvaluations.stream()
        .map(evaluation -> {
          ProspectEvaluationInfo evaluationInfo = evaluation.getProspectEvaluationInfo();
          return prospectMapper.fromEvaluationInfos(null, null, evaluationInfo, null);
        }).collect(Collectors.toList());

    return repository.createAll(prospects);
  }

  private void validateStatusUpdateFlow(Prospect toSave, Prospect existing) {
    StringBuilder exceptionBuilder = new StringBuilder();
    if (toSave.getActualStatus() == TO_CONTACT && existing.getActualStatus() != CONTACTED) {
      exceptionBuilder.append("Prospect(id=").append(toSave.getId()).append(",status=")
          .append(toSave.getActualStatus()).append(") can only be updated to status ")
          .append(CONTACTED);
    } else if (toSave.getActualStatus() == CONTACTED && existing.getActualStatus() != CONVERTED) {
      exceptionBuilder.append("Prospect(id=").append(toSave.getId()).append(",status=")
          .append(toSave.getActualStatus()).append(") can only be updated to status ")
          .append(CONVERTED);
    }
    String errorMsg = exceptionBuilder.toString();
    if (!errorMsg.isEmpty()) {
      throw new BadRequestException(errorMsg);
    }
  }
}