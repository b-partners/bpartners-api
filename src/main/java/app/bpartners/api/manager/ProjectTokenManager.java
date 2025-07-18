package app.bpartners.api.manager;

import app.bpartners.api.endpoint.event.SsmComponent;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

@Component
@EnableAsync
public class ProjectTokenManager {
  public static final int FOURTY_FIVE_MINUTES_INTERVAL = 2700000;
  public static final String PARAMS_NAME_GOOGLE_SERVICE_ACCOUNT =
      "/bpartners/google/service-account";
  private final SsmComponent ssmComponent;
  private final String env;

  public ProjectTokenManager(SsmComponent ssmComponent, @Value("${env}") String env) {
    this.ssmComponent = ssmComponent;
    this.env = env;
  }

  public InputStream googleServiceAccountStream() {
    String serviceAccountValue = ssmComponent.getParameterValue(PARAMS_NAME_GOOGLE_SERVICE_ACCOUNT);
    return new ByteArrayInputStream(serviceAccountValue.getBytes());
  }
}
