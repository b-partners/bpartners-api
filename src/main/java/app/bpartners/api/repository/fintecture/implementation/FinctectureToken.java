package app.bpartners.api.repository.fintecture.implementation;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.fintecture.model.TokenInitiation;
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
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class FinctectureToken {
  private final FintectureConf fintectureConf;
  private final TokenInitiation tokenInitiation;

  @Value("${fintecture.app.secret}")
  private final String APP_SECRET;

  public TokenResponse get() {
    try {
      HttpClient httpClient = HttpClient.newBuilder().build();
      HttpRequest.BodyPublisher data = getParamsUrlEncoded(new ObjectMapper()
          .convertValue(tokenInitiation, Map.class));
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(fintectureConf.getConnectPisUrl()))
          .header("Content-Type", "application/x-www-form-urlencoded")
          .header("Accept", "application/json")
          .header("Authorization", Base64.getEncoder()
              .encodeToString((tokenInitiation.getAppId() + APP_SECRET).getBytes()))
          .POST(data)
          .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      return new ObjectMapper()
          .findAndRegisterModules()
          .readValue(response.body(), TokenResponse.class);
    } catch (IOException | InterruptedException | URISyntaxException e) {
      throw new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, e);
    }
  }

  private HttpRequest.BodyPublisher getParamsUrlEncoded(Map<String, String> parameters) {
    String urlEncoded = parameters.entrySet()
        .stream()
        .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
        .collect(Collectors.joining("&"));
    return HttpRequest.BodyPublishers.ofString(urlEncoded);
  }

}
