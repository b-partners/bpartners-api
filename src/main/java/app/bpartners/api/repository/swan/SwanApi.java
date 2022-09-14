package app.bpartners.api.repository.swan;

import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.endpoint.rest.security.principal.PrincipalProvider;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.swan.response.ProjectTokenResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import static app.bpartners.api.endpoint.rest.security.swan.SwanConf.BEARER_PREFIX;
import static app.bpartners.api.endpoint.rest.security.swan.SwanConf.SWAN_TOKEN_URL;

/*
 *
 * T generic type for SWAN GraphQL responses
 * ex : AccountHolderResponse or AccountResponse
 *
 * */


@AllArgsConstructor
@Component
public class SwanApi<T> {
  private final PrincipalProvider auth;
  private SwanConf swanConf;

  public T getData(Class<T> genericClass, String message) {
    try {
      HttpClient httpClient = HttpClient.newBuilder().build();
      HttpRequest request = HttpRequest.newBuilder().uri(new URI(swanConf.getApiUrl()))
          .header("Content-Type", "application/json")
          .header("Authorization", BEARER_PREFIX + bearerToken())
          .POST(HttpRequest.BodyPublishers.ofString(message)).build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      return new ObjectMapper().findAndRegisterModules()//Load DateTime Module
          .readValue(response.body(), genericClass);
    } catch (IOException | InterruptedException | URISyntaxException e) {
      throw new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, e);
    }
  }

  private String bearerToken() {
    return ((Principal) auth.getAuthentication().getPrincipal()).getBearer();
  }

  public ProjectTokenResponse getProjectToken() {
    try {
      HttpClient httpClient = HttpClient.newBuilder().build();
      HttpRequest request = HttpRequest.newBuilder().uri(new URI(SWAN_TOKEN_URL))
          .header("Content-Type", "application/x-www-form-urlencoded")
          .POST(getParamsUrlEncoded(swanConf.getParams())).build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      return new ObjectMapper().findAndRegisterModules()//Load DateTime Module
          .readValue(response.body(), ProjectTokenResponse.class);
    } catch (InterruptedException | IOException | URISyntaxException e) {
      throw new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, e);
    }
  }
  /*
    TODO: if getProjectToken fails you should get a new Token
   */

  private HttpRequest.BodyPublisher getParamsUrlEncoded(Map<String, String> parameters) {
    String urlEncoded = parameters.entrySet()
        .stream()
        .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
        .collect(Collectors.joining("&"));
    return HttpRequest.BodyPublishers.ofString(urlEncoded);
  }
}
