package app.bpartners.api.manager;

import app.bpartners.api.model.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.SsmException;

@Component
public class ProjectTokenManager {
  private SsmClient ssmClient;
  private final String projectParameterName;

  public ProjectTokenManager(SsmClient ssmClient,
                             @Value("${aws.ssm.swan.project.param}")
                             String projectParameterName) {
    this.ssmClient = ssmClient;
    this.projectParameterName = projectParameterName;
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
    return getParameterValue(ssmClient, projectParameterName);
  }
}
