package app.bpartners.api.model.project;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AnalysisProjectStatus {
  String projectKey;
  String externalReference;
  String detectionIdentifier;
  String zone;
  String address;
  String initiatorName;
  String initiatorEmail;
  String initiatorPhoneNumber;
  Instant creationDatetime;
  boolean analysis2dDone;
  boolean model3dDone;
  boolean creditConsumed;
  String creditTransactionId;
  List<AnalysisProjectStep> remainingSteps;
  long creditCost;
  boolean creditsSufficient;

  public static AnalysisProjectStatus of(
      AnalysisProject project,
      String projectKey,
      String externalReference,
      long creditCost,
      long spendableCredits) {
    var known = project != null;
    var creditConsumed = known && project.isCreditConsumed();
    var builder =
        AnalysisProjectStatus.builder()
            .projectKey(known ? project.getProjectKey() : projectKey)
            .externalReference(known ? project.getExternalReference() : externalReference)
            .analysis2dDone(known && project.isAnalysis2dDone())
            .model3dDone(known && project.isModel3dDone())
            .creditConsumed(creditConsumed)
            .creditTransactionId(known ? project.getCreditTransactionId() : null)
            .remainingSteps(
                known
                    ? project.remainingSteps()
                    : List.of(AnalysisProjectStep.ANALYSIS_2D, AnalysisProjectStep.MODEL_3D))
            .creditCost(creditConsumed ? 0L : creditCost)
            .creditsSufficient(creditConsumed || spendableCredits >= creditCost);
    if (known) {
      builder
          .detectionIdentifier(project.getDetectionIdentifier())
          .zone(project.getZone())
          .address(project.getAddress())
          .initiatorName(project.getInitiatorName())
          .initiatorEmail(project.getInitiatorEmail())
          .initiatorPhoneNumber(project.getInitiatorPhoneNumber())
          .creationDatetime(project.getCreationDatetime());
    }
    return builder.build();
  }
}
