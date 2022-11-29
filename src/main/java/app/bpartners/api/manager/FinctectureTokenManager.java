package app.bpartners.api.manager;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.fintecture.model.TokenResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.repository.fintecture.FintectureConf.PIS_SCOPE;

@Component
@AllArgsConstructor
public class FinctectureTokenManager {
  private final FintectureConf fintectureConf;
  private static final String BASIC_PREFIX = "Basic ";

  public TokenResponse getProjectAccessToken() {
    try {
      HttpClient httpClient = HttpClient.newBuilder().build();
      HttpRequest.BodyPublisher data = getParamsUrlEncoded(tokenMap());
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(fintectureConf.getOauthUrl()))
          .header("Content-Type", "application/x-www-form-urlencoded")
          .header("Accept", "application/json")
          .header("Authorization", BASIC_PREFIX + Base64.getEncoder()
              .encodeToString(
                  (fintectureConf.getAppId() + ":" + fintectureConf.getAppSecret()).getBytes()))
          .POST(data)
          .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      return new ObjectMapper()
          .findAndRegisterModules()
          .readValue(response.body(), TokenResponse.class);
    } catch (IOException | URISyntaxException e) {
      throw new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  private HttpRequest.BodyPublisher getParamsUrlEncoded(Map<String, String> parameters) {
    String urlEncoded = parameters.entrySet()
        .stream()
        .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
        .collect(Collectors.joining("&"));
    return HttpRequest.BodyPublishers.ofString(urlEncoded);
  }

  private Map<String, String> tokenMap() {
    Map<String, String> token = new HashMap<>();
    token.put("grant_type", "client_credentials");
    token.put("app_id", fintectureConf.getAppId());
    token.put("scope", PIS_SCOPE);
    return token;
  }
}
