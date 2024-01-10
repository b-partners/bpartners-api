package app.bpartners.api.repository.expressif;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExpressifConf {
  @Getter private final String projectToken;
  private final String baseUrl;
  private final Integer port;

  public ExpressifConf(
      @Value("${expressif.project.token}") String projectToken,
      @Value("${expressif.port}") Integer port,
      @Value("${expressif.base.url}") String baseUrl) {
    this.projectToken = projectToken;
    this.baseUrl = baseUrl;
    this.port = port;
  }

  private String baseUrl() {
    return baseUrl + ":" + port;
  }

  public String getProcessUrl(Map<String, String> queryParams) {
    String params =
        queryParams.entrySet().stream()
            .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));
    String url = baseUrl() + "/api/sync/process";
    return queryParams.isEmpty() ? url : url + "?" + params;
  }
}
