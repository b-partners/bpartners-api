package app.bpartners.api.manager;

import app.bpartners.api.endpoint.event.SsmComponent;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableAsync
public class ProjectTokenManager {
  public static final int FOURTY_FIVE_MINUTES_INTERVAL = 2700000;
  public static final String PARAMS_NAME_GOOGLE_SERVICE_ACCOUNT = "/bpartners/google/service-account";
  private final FinctectureTokenManager finctectureTokenManager;
  private final SsmComponent ssmComponent;
  private final String env;

  public ProjectTokenManager(SsmComponent ssmComponent,
                             FinctectureTokenManager finctectureTokenManager,
                             @Value("${env}") String env) {
    this.ssmComponent = ssmComponent;
    this.finctectureTokenManager = finctectureTokenManager;
    this.env = env;
  }

  public String getFintectureProjectToken() {
    return ssmComponent.getParameterValue(getFintectureTokenParameterName());
  }

  public InputStream googleServiceAccountStream() {
    String serviceAccountValue =
        ssmComponent.getParameterValue(PARAMS_NAME_GOOGLE_SERVICE_ACCOUNT);
    return new ByteArrayInputStream(serviceAccountValue.getBytes());
  }

  /*TODO: retry to get token after 10 secondes in case of server failure*/
  @Async
  @PostConstruct
  public void refreshFintectureProjectToken() {
    ssmComponent.setParameterStringValue(
        getFintectureTokenParameterName(),
        finctectureTokenManager.getProjectAccessToken().getAccessToken());
  }

  private String getFintectureTokenParameterName() {
    return "/bpartners/" + env + "/fintecture/project-access-token";
  }
}
