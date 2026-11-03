package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.AnalysisProject;
import app.bpartners.api.endpoint.rest.model.AnalysisProjectStep;
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
}
