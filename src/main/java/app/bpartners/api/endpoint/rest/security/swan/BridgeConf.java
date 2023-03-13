package app.bpartners.api.endpoint.rest.security.swan;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class BridgeConf {
  private String clientId;
  private String clientSecret;
  private String baseUrl;
  private String bridgeVersion;

  public BridgeConf(
      @Value("${bridge.client.id}")
      String clientId,
      @Value("${bridge.client.secret}")
      String clientSecret,
      @Value("${bridge.base.url}")
      String baseUrl,
      @Value("${bridge.version}")
      String bridgeVersion
  ) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.baseUrl = baseUrl;
    this.bridgeVersion = bridgeVersion;
  }

  public String getUserUrl() {
    return baseUrl + "/users";
  }
}
