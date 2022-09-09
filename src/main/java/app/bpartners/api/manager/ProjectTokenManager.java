package app.bpartners.api.manager;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.fintecture.implementation.FinctectureToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.PutParameterRequest;
import software.amazon.awssdk.services.ssm.model.SsmException;

@Component
@EnableScheduling
public class ProjectTokenManager {
  private final String swanProjectParamName;
  private final String fintectureProjectParamName;
  private final SsmClient ssmClient;
  private final FinctectureToken finctectureToken;

  public ProjectTokenManager(SsmClient ssmClient,
                             @Value("${aws.ssm.swan.project.param}")
                             String swanProjectParamName,
                             @Value("${fintecture.project.param.name}")
                             String fintectureProjectParamName, FinctectureToken finctectureToken) {
    this.ssmClient = ssmClient;
    this.swanProjectParamName = swanProjectParamName;
    this.fintectureProjectParamName = fintectureProjectParamName;
    this.finctectureToken = finctectureToken;
  }

  private String getParameterValue(SsmClient ssmClient, String parameterName) {
    try {
      GetParameterRequest parameterRequest = GetParameterRequest.builder()
          .name(parameterName)
          .build();

      GetParameterResponse parameterResponse = ssmClient.getParameter(parameterRequest);

      return parameterResponse.parameter().value();
    } catch (SsmException e) {
      throw new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, e);
    }
  }

  @Scheduled(fixedDelay = 60 * 60 * 1000 - 10000)
  public void setFintectureProjectToken() {
    ssmClient.putParameter(PutParameterRequest.builder()
        .name(fintectureProjectParamName)
        .value(finctectureToken.get().getAccess_token())
        .type("String")
        .build());
  }

  public String getSwanProjecToken() {
    return getParameterValue(ssmClient, swanProjectParamName);
  }

  public String getFintectureProjectToken() {
    return getParameterValue(ssmClient, fintectureProjectParamName);
  }
}
