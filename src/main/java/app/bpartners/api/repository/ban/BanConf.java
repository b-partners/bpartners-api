package app.bpartners.api.repository.ban;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/*
 BAN stands for Base Adresse Nationale.
 It is a public API own by FR government for accessing to address.
 */
@Configuration
public class BanConf {
  @Getter
  private final String baseUrl;

  public BanConf(
      @Value("${ban.base.url}") String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getSearchUrl(Map<String, String> queryParams) {
    String params = queryParams.entrySet()
        .stream()
        .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
        .collect(Collectors.joining("&"));
    String url = baseUrl + "/search/";
    return queryParams.isEmpty() ? url : url + "?" + params;
  }
}
