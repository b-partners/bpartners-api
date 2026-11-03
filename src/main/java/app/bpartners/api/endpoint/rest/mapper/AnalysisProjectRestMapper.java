package app.bpartners.api.endpoint.rest.mapper;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.rest.model.AnalysisProject;
import app.bpartners.api.endpoint.rest.model.AnalysisProjectStep;
import app.bpartners.api.endpoint.rest.model.Register2d;
import app.bpartners.api.model.User;
import app.bpartners.api.model.detection.DetectionInitiator;
import app.bpartners.api.model.detection.DetectionTracking;
import app.bpartners.api.model.project.AnalysisProjectStatus;
import org.springframework.stereotype.Component;

@Component
public class AnalysisProjectRestMapper {

  public AnalysisProject toRest(AnalysisProjectStatus status) {
    return new AnalysisProject()
        .projectKey(status.getProjectKey())
        .externalReference(status.getExternalReference())
        .analysis2dDone(status.isAnalysis2dDone())
        .model3dDone(status.isModel3dDone())
        .creditConsumed(status.isCreditConsumed())
        .creditTransactionId(status.getCreditTransactionId())
        .remainingSteps(
            status.getRemainingSteps().stream()
                .map(step -> AnalysisProjectStep.valueOf(step.name()))
                .toList())
        .creditCost(status.getCreditCost())
        .creditsSufficient(status.isCreditsSufficient());
  }

  public DetectionTracking toTracking(String projectKey, Register2d rest, User user) {
    var initiator = rest.getInitiator();
    return new DetectionTracking(
        randomUUID().toString(),
        rest.getZone(),
        rest.getAddress(),
        rest.getCreationDatetime() == null ? now() : rest.getCreationDatetime(),
        initiator == null
            ? new DetectionInitiator(null, null, null)
            : new DetectionInitiator(
                initiator.getName(), initiator.getEmail(), initiator.getPhoneNumber()),
        user,
        rest.getDetectionIdentifier(),
        projectKey,
        rest.getExternalReference());
  }
}
