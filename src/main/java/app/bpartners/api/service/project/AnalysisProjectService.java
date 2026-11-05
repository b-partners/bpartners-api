package app.bpartners.api.service.project;

import static app.bpartners.api.model.activity.UserActivityType.ROOF_MODEL_3D;
import static app.bpartners.api.model.activity.UserActivityUnit.UNIT;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.activity.UserActivity;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.project.AnalysisProject;
import app.bpartners.api.model.project.AnalysisProjectDescription;
import app.bpartners.api.model.project.AnalysisProjectPhase;
import app.bpartners.api.model.project.AnalysisProjectStatus;
import app.bpartners.api.model.project.AnalysisProjectStep;
import app.bpartners.api.repository.jpa.AnalysisProjectRepository;
import app.bpartners.api.repository.jpa.CreditTransactionRepository;
import app.bpartners.api.service.activity.UserActivityService;
import app.bpartners.api.service.credit.CreditService;
import app.bpartners.api.service.subscription.PrimaryUserResolver;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalysisProjectService {
  private static final int DEFAULT_PAGE_SIZE = 100;
  private final AnalysisProjectRepository repository;
  private final CreditTransactionRepository creditTransactionRepository;
  private final CreditService creditService;
  private final PrimaryUserResolver primaryUserResolver;
  private final UserActivityService userActivityService;

  @Transactional
  public AnalysisProject register(
      String actorUserId,
      String projectKey,
      String externalReference,
      AnalysisProjectStep step,
      String label,
      AnalysisProjectDescription description) {
    if (isBlank(projectKey) && isBlank(externalReference)) {
      throw new BadRequestException("Either projectKey or externalReference is mandatory.");
    }
    var userId = primaryUserResolver.resolve(actorUserId);
    creditTransactionRepository.acquireWalletLock(userId);

    var project =
        find(userId, projectKey, externalReference)
            .orElseGet(() -> newProject(userId, projectKey, externalReference));
    if (project.isDone(step)) {
      return project;
    }

    var registered = project.withStepDone(step, now()).withDescription(description);
    if (!registered.isCreditConsumed()) {
      var transaction = creditService.consumeRoofAnalysis(actorUserId, label);
      registered =
          registered.toBuilder()
              .creditConsumedDatetime(now())
              .creditTransactionId(transaction.getId())
              .build();
    }
    var saved = repository.save(withAlias(registered, externalReference));
    if (step == AnalysisProjectStep.MODEL_3D) {
      recordModel3d(userId, label);
    }
    return saved;
  }

  private void recordModel3d(String userId, String label) {
    userActivityService.record(
        UserActivity.builder()
            .id(randomUUID().toString())
            .userId(userId)
            .activityType(ROOF_MODEL_3D)
            .activityUnit(UNIT)
            .quantity(1L)
            .description(label)
            .creationDatetime(now())
            .build());
  }

  @Transactional
  public AnalysisProject createDraft(
      String actorUserId,
      String projectKey,
      String externalReference,
      AnalysisProjectDescription description,
      String areaPictureId,
      String prospectId) {
    if (isBlank(projectKey)) {
      throw new BadRequestException("projectKey is mandatory.");
    }
    var userId = primaryUserResolver.resolve(actorUserId);
    creditTransactionRepository.acquireWalletLock(userId);

    var existing = find(userId, projectKey, externalReference);
    if (existing.isPresent() && existing.get().phase() != AnalysisProjectPhase.DRAFT) {
      return existing.get();
    }
    var draft =
        existing
            .orElseGet(() -> newProject(userId, projectKey, externalReference))
            .withDescription(description);
    return repository.save(
        withAlias(
            draft.toBuilder()
                .areaPictureId(firstNonBlank(areaPictureId, draft.getAreaPictureId()))
                .prospectId(firstNonBlank(prospectId, draft.getProspectId()))
                .build(),
            externalReference));
  }

  @Transactional(readOnly = true)
  public AnalysisProjectStatus getStatus(
      String actorUserId, String projectKey, String externalReference) {
    if (isBlank(projectKey) && isBlank(externalReference)) {
      throw new BadRequestException("Either projectKey or externalReference is mandatory.");
    }
    var userId = primaryUserResolver.resolve(actorUserId);
    var balance = creditService.getCreditBalance(userId);
    return AnalysisProjectStatus.of(
        find(userId, projectKey, externalReference).orElse(null),
        projectKey,
        externalReference,
        balance.getCreditCostPerAnalysis(),
        balance.getSpendableCredits());
  }

  @Transactional(readOnly = true)
  public List<AnalysisProjectStatus> list(
      String actorUserId,
      String projectKey,
      String externalReference,
      String search,
      AnalysisProjectPhase status,
      PageFromOne page,
      BoundedPageSize pageSize) {
    var userId = primaryUserResolver.resolve(actorUserId);
    var balance = creditService.getCreditBalance(userId);
    var pageValue = page != null ? page.getValue() - 1 : 0;
    var pageSizeValue = pageSize != null ? pageSize.getValue() : DEFAULT_PAGE_SIZE;
    return repository
        .findAllByUserIdAndFilters(
            userId,
            blankToNull(projectKey),
            blankToNull(externalReference),
            blankToNull(search),
            status == null ? null : status.name(),
            PageRequest.of(pageValue, pageSizeValue))
        .stream()
        .map(
            project ->
                AnalysisProjectStatus.of(
                    project,
                    project.getProjectKey(),
                    project.getExternalReference(),
                    balance.getCreditCostPerAnalysis(),
                    balance.getSpendableCredits()))
        .toList();
  }

  private Optional<AnalysisProject> find(
      String userId, String projectKey, String externalReference) {
    var byReference =
        isBlank(externalReference)
            ? Optional.<AnalysisProject>empty()
            : repository.findByUserIdAndExternalReference(userId, externalReference);
    if (byReference.isPresent() || isBlank(projectKey)) {
      return byReference;
    }
    return repository.findByUserIdAndProjectKey(userId, projectKey);
  }

  private AnalysisProject newProject(String userId, String projectKey, String externalReference) {
    return AnalysisProject.builder()
        .id(randomUUID().toString())
        .userId(userId)
        .projectKey(isBlank(projectKey) ? "reference:" + externalReference : projectKey)
        .externalReference(externalReference)
        .creationDatetime(now())
        .build();
  }

  private AnalysisProject withAlias(AnalysisProject project, String externalReference) {
    if (isBlank(externalReference) || project.getExternalReference() != null) {
      return project;
    }
    return project.toBuilder().externalReference(externalReference).build();
  }

  private String firstNonBlank(String candidate, String current) {
    return isBlank(candidate) ? current : candidate;
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private String blankToNull(String value) {
    return isBlank(value) ? null : value;
  }
}
