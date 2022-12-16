package app.bpartners.api.manager;

import app.bpartners.api.endpoint.event.SsmComponent;
import app.bpartners.api.repository.swan.SwanApi;
import app.bpartners.api.repository.swan.response.ProjectTokenResponse;
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
  private final String swanProjectParamName;
  private final String fintectureProjectParamName;
  private final SwanApi<ProjectTokenResponse> swanApi;
  private final FinctectureTokenManager finctectureTokenManager;
  private final SsmComponent ssmComponent;

  public ProjectTokenManager(SsmComponent ssmComponent,
                             @Value("${aws.ssm.swan.project.param}")
                             String swanProjectParamName,
                             @Value("${aws.ssm.fintecture.project.param}")
                             String fintectureProjectParamName,
                             SwanApi<ProjectTokenResponse> swanApi,
                             FinctectureTokenManager finctectureTokenManager) {
    this.ssmComponent = ssmComponent;
    this.swanProjectParamName = swanProjectParamName;
    this.fintectureProjectParamName = fintectureProjectParamName;
    this.finctectureTokenManager = finctectureTokenManager;
    this.swanApi = swanApi;
  }

  public String getSwanProjecToken() {
    return ssmComponent.getParameterValue(swanProjectParamName);
  }

  public String getFintectureProjectToken() {
    return ssmComponent.getParameterValue(fintectureProjectParamName);
  }

  @Scheduled(fixedRate = FOURTY_FIVE_MINUTES_INTERVAL)
  @Async
  @PostConstruct
  public void refreshSwanProjectToken() {
    ssmComponent.setParameterStringValue(
        swanProjectParamName, swanApi.getProjectToken().getAccessToken());
  }

  /*TODO: retry to get token after 10 secondes in case of server failure*/
  @Scheduled(fixedRate = FOURTY_FIVE_MINUTES_INTERVAL)
  @Async
  @PostConstruct
  public void refreshFintectureProjectToken() {
    ssmComponent.setParameterStringValue(
        fintectureProjectParamName,
        finctectureTokenManager.getProjectAccessToken().getAccessToken());
  }
}
