package app.bpartners.api.repository.fintecture;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class FintectureConf {
  private String appId;
  private String appSecret;
  private String baseUrl;
  private String apiVersion;
  private final String pisService = "pis";
  private final String aisService = "ais";


  public FintectureConf(
      @Value("${fintecture.app.id}")
      String appId,
      @Value("${fintecture.app.secret}")
      String appSecret,
      @Value("${fintecture.base.url}")
      String baseUrl,
      @Value("${fintecture.api.version}")
      String apiVersion) {
    this.appId = appId;
    this.appSecret = appSecret;
    this.baseUrl = baseUrl;
    this.apiVersion = apiVersion;
  }

  public String getConnectEndpointUrl(String service) {
    return String.format(baseUrl + "/%s/%s/connect", service, apiVersion);
  }

  public String getConnectPisUrl() {
    return getConnectEndpointUrl(pisService);
  }
}
