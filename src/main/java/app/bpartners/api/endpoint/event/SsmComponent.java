package app.bpartners.api.endpoint.event;

import app.bpartners.api.model.exception.ApiException;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.PutParameterRequest;
import software.amazon.awssdk.services.ssm.model.SsmException;

@Component
public class SsmComponent {
  private final SsmClient ssmClient;
  private static final String SSM_STRING_PARAMETER_TYPE = "String";

  public SsmComponent(SsmClient ssmClient) {
    this.ssmClient = ssmClient;
  }

  public String getParameterValue(String parameterName) {
    try {
      GetParameterRequest parameterRequest =
          GetParameterRequest.builder().name(parameterName).build();

      GetParameterResponse parameterResponse = ssmClient.getParameter(parameterRequest);

      return parameterResponse.parameter().value();
    } catch (SsmException e) {
      throw new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, e);
    }
  }

  public void setParameterStringValue(String parameterName, String value) {
    ssmClient.putParameter(
        PutParameterRequest.builder()
            .name(parameterName)
            .value(value)
            .type(SSM_STRING_PARAMETER_TYPE)
            .overwrite(true)
            .build());
  }
}
