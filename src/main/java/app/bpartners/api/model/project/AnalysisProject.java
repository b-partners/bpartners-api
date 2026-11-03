package app.bpartners.api.model.project;

import static app.bpartners.api.model.project.AnalysisProjectStep.ANALYSIS_2D;
import static app.bpartners.api.model.project.AnalysisProjectStep.MODEL_3D;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity(name = "analysis_project")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@ToString
public class AnalysisProject {
  @Id private String id;

  @Column(name = "user_id")
  private String userId;

  @Column(name = "project_key")
  private String projectKey;

  @Column(name = "external_reference")
  private String externalReference;

  @Column(name = "detection_identifier")
  private String detectionIdentifier;

  private String zone;

  private String address;

  @Column(name = "initiator_name")
  private String initiatorName;

  @Column(name = "initiator_email")
  private String initiatorEmail;

  @Column(name = "initiator_phone_number")
  private String initiatorPhoneNumber;

  @Column(name = "analysis_2d_datetime")
  private Instant analysis2dDatetime;

  @Column(name = "model_3d_datetime")
  private Instant model3dDatetime;

  @Column(name = "credit_consumed_datetime")
  private Instant creditConsumedDatetime;

  @Column(name = "credit_transaction_id")
  private String creditTransactionId;

  @Column(name = "creation_datetime")
  private Instant creationDatetime;

  public boolean isAnalysis2dDone() {
    return analysis2dDatetime != null;
  }

  public boolean isModel3dDone() {
    return model3dDatetime != null;
  }

  public boolean isCreditConsumed() {
    return creditConsumedDatetime != null;
  }

  public boolean isDone(AnalysisProjectStep step) {
    return switch (step) {
      case ANALYSIS_2D -> isAnalysis2dDone();
      case MODEL_3D -> isModel3dDone();
    };
  }

  public List<AnalysisProjectStep> remainingSteps() {
    var remaining = new ArrayList<AnalysisProjectStep>();
    if (!isAnalysis2dDone()) {
      remaining.add(ANALYSIS_2D);
    }
    if (!isModel3dDone()) {
      remaining.add(MODEL_3D);
    }
    return remaining;
  }

  public AnalysisProject withDescription(AnalysisProjectDescription description) {
    if (description == null) {
      return this;
    }
    return toBuilder()
        .detectionIdentifier(firstNonNull(description.detectionIdentifier(), detectionIdentifier))
        .zone(firstNonNull(description.zone(), zone))
        .address(firstNonNull(description.address(), address))
        .initiatorName(firstNonNull(description.initiatorName(), initiatorName))
        .initiatorEmail(firstNonNull(description.initiatorEmail(), initiatorEmail))
        .initiatorPhoneNumber(
            firstNonNull(description.initiatorPhoneNumber(), initiatorPhoneNumber))
        .build();
  }

  private static String firstNonNull(String candidate, String current) {
    return candidate == null ? current : candidate;
  }

  public AnalysisProject withStepDone(AnalysisProjectStep step, Instant datetime) {
    return switch (step) {
      case ANALYSIS_2D -> toBuilder().analysis2dDatetime(datetime).build();
      case MODEL_3D -> toBuilder().model3dDatetime(datetime).build();
    };
  }
}
