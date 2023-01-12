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
  private final SwanApi<ProjectTokenResponse> swanApi;
  private final FinctectureTokenManager finctectureTokenManager;
  private final SsmComponent ssmComponent;
  private final String env;

  public ProjectTokenManager(SsmComponent ssmComponent,
                             SwanApi<ProjectTokenResponse> swanApi,
                             FinctectureTokenManager finctectureTokenManager,
                             @Value("${env}") String env) {
    this.ssmComponent = ssmComponent;
    this.finctectureTokenManager = finctectureTokenManager;
    this.swanApi = swanApi;
    this.env = env;
  }

  public String getSwanProjecToken() {
    return ssmComponent.getParameterValue(getSwanTokenParameterName());
  }

  public String getFintectureProjectToken() {
    return ssmComponent.getParameterValue(getFintectureTokenParameterName());
  }

  @Scheduled(fixedRate = FOURTY_FIVE_MINUTES_INTERVAL)
  @Async
  @PostConstruct
  public void refreshSwanProjectToken() {
//    ssmComponent.setParameterStringValue(
//        getSwanTokenParameterName(),
//        swanApi.getProjectToken().getAccessToken());
  }

  /*TODO: retry to get token after 10 secondes in case of server failure*/
  @Scheduled(fixedRate = FOURTY_FIVE_MINUTES_INTERVAL)
  @Async
  @PostConstruct
  public void refreshFintectureProjectToken() {
//    ssmComponent.setParameterStringValue(
//        getFintectureTokenParameterName(),
//        finctectureTokenManager.getProjectAccessToken().getAccessToken());
  }

  private String getFintectureTokenParameterName() {
    return "/bpartners/" + env + "/fintecture/project-access-token";
  }

  private String getSwanTokenParameterName() {
    return "/bpartners/" + env + "/swan/project-access-token";
  }
}
