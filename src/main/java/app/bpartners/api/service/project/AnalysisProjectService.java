package app.bpartners.api.service.project;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.project.AnalysisProject;
import app.bpartners.api.model.project.AnalysisProjectStatus;
import app.bpartners.api.model.project.AnalysisProjectStep;
import app.bpartners.api.repository.jpa.AnalysisProjectRepository;
import app.bpartners.api.repository.jpa.CreditTransactionRepository;
import app.bpartners.api.service.credit.CreditService;
import app.bpartners.api.service.subscription.PrimaryUserResolver;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalysisProjectService {
  private final AnalysisProjectRepository repository;
  private final CreditTransactionRepository creditTransactionRepository;
  private final CreditService creditService;
  private final PrimaryUserResolver primaryUserResolver;

  @Transactional
  public AnalysisProject register(
      String actorUserId,
      String projectKey,
      String externalReference,
      AnalysisProjectStep step,
      String label) {
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

    var registered = project.withStepDone(step, now());
    if (!registered.isCreditConsumed()) {
      var transaction = creditService.consumeRoofAnalysis(actorUserId, label);
      registered =
          registered.toBuilder()
              .creditConsumedDatetime(now())
              .creditTransactionId(transaction.getId())
              .build();
    }
    return repository.save(withAlias(registered, externalReference));
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

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
