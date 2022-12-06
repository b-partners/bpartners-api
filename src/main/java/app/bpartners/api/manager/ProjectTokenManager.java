package app.bpartners.api.manager;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.swan.SwanApi;
import app.bpartners.api.repository.swan.response.ProjectTokenResponse;
import javax.annotation.PostConstruct;
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
  private static final String SSM_STRING_PARAMETER_TYPE = "String";
  private final String swanProjectParamName;
  private final String fintectureProjectParamName;
  private final SwanApi<ProjectTokenResponse> swanApi;
  private final FinctectureTokenManager finctectureTokenManager;
  private final SsmClient ssmClient;

  public ProjectTokenManager(SsmClient ssmClient,
                             @Value("${aws.ssm.swan.project.param}")
                             String swanProjectParamName,
                             @Value("${aws.ssm.fintecture.project.param}")
                             String fintectureProjectParamName,
                             SwanApi<ProjectTokenResponse> swanApi,
                             FinctectureTokenManager finctectureTokenManager) {
    this.ssmClient = ssmClient;
    this.swanProjectParamName = swanProjectParamName;
    this.fintectureProjectParamName = fintectureProjectParamName;
    this.finctectureTokenManager = finctectureTokenManager;
    this.swanApi = swanApi;
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

  public String getSwanProjecToken() {
    return getParameterValue(ssmClient, swanProjectParamName);
  }

  public String getFintectureProjectToken() {
    return getParameterValue(ssmClient, fintectureProjectParamName);
  }

  @Scheduled(cron = "0 0 * * * ?")
  @PostConstruct
  public void refreshSwanProjectToken() {
    String accessToken =
        swanApi.getProjectToken().getAccessToken();
    ssmClient.putParameter(PutParameterRequest
        .builder()
        .name(swanProjectParamName)
        .value(accessToken)
        .type(SSM_STRING_PARAMETER_TYPE)
        .overwrite(true)
        .build()
    );
  }

  /*TODO: retry to get token after 10 secondes in case of server failure*/
  @Scheduled(cron = "0 0 * * * ?")
  @PostConstruct
  public void refreshFintectureProjectToken() {
    ssmClient.putParameter(PutParameterRequest.builder()
        .name(fintectureProjectParamName)
        .value(finctectureTokenManager.getProjectAccessToken().getAccessToken())
        .type(SSM_STRING_PARAMETER_TYPE)
        .overwrite(true)
        .build());
  }
}
