package app.bpartners.api.repository.fintecture;

import app.bpartners.api.endpoint.event.SsmComponent;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import static org.apache.logging.log4j.util.Strings.EMPTY;

@Configuration
@Getter
public class FintectureConf {
  private String appId;
  private String appSecret;
  private String baseUrl;
  private String apiVersion;
  private SsmComponent ssmComponent;
  private String env;
  private static final String PIS_SERVICE = "pis";
  public static final String PIS_SCOPE = "PIS";


  public FintectureConf(
      @Value("${fintecture.app.id}")
      String appId,
      @Value("${fintecture.app.secret}")
      String appSecret,
      @Value("${fintecture.base.url}")
      String baseUrl,
      @Value("${fintecture.api.version}")
      String apiVersion,
      SsmComponent ssmComponent,
      @Value("${env}") String env) {
    this.appId = appId;
    this.appSecret = appSecret;
    this.baseUrl = baseUrl;
    this.apiVersion = apiVersion;
    this.ssmComponent = ssmComponent;
    this.env = env;
  }

  public String getPrivateKey() {
    String endPrivateKeySyntax = "-----END PRIVATE KEY-----";
    String beginPrivateKeySyntax = "-----BEGIN PRIVATE KEY-----";
    String spaceAndTabRegex = "\\r\\n|\\r|\\n";
    String privateKey =
        ssmComponent.getParameterValue("/bpartners/" + env + "/fintecture/private-key");
    return privateKey.replace(endPrivateKeySyntax, EMPTY)
        .replace(beginPrivateKeySyntax, EMPTY)
        .replaceAll(spaceAndTabRegex, EMPTY);
  }

  public String getRequestToPayUrl() {
    return String.format("%s/%s/%s/request-to-pay", baseUrl, PIS_SERVICE, apiVersion);
  }

  public String getOauthUrl() {
    return baseUrl + "/oauth/accesstoken";
  }
}
