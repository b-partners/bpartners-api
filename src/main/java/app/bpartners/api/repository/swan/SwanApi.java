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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static app.bpartners.api.endpoint.rest.security.swan.SwanConf.SWAN_TOKEN_URL;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

/*
 *
 * T generic type for SWAN GraphQL responses
 * ex : AccountHolderResponse or AccountResponse
 *
 * */


@Component
@Slf4j
public class SwanApi<T> {
  private final PrincipalProvider auth;
  private final SwanCustomApi<T> swanCustomApi;
  private final SwanConf swanConf;

  public SwanApi(PrincipalProvider auth, SwanCustomApi<T> swanCustomApi, SwanConf swanConf) {
    this.auth = auth;
    this.swanCustomApi = swanCustomApi;
    this.swanConf = swanConf;
  }

  public T getData(Class<T> genericClass, String message) {
    return swanCustomApi.getData(genericClass, message, bearerToken());
  }

  private String bearerToken() {
    return ((Principal) auth.getAuthentication().getPrincipal()).getBearer();
  }

  public ProjectTokenResponse getProjectToken(int n, HttpClient httpClient, long ms) {
    ProjectTokenResponse projectTokenResponse;
    try {
      HttpRequest request = HttpRequest.newBuilder().uri(new URI(SWAN_TOKEN_URL))
          .header("Content-Type", "application/x-www-form-urlencoded")
          .POST(getParamsUrlEncoded(swanConf.getParams())).build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      projectTokenResponse = new ObjectMapper().findAndRegisterModules()//Load DateTime Module
          .readValue(response.body(), ProjectTokenResponse.class);
    } catch (IOException | URISyntaxException e) {
      if (n == 3) {
        throw new ApiException(SERVER_EXCEPTION, e);
      }
      makeThreadWait(Thread.currentThread(), ms, n);
      return getProjectToken(++n, httpClient, ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
    return projectTokenResponse;
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

  private void makeThreadWait(Thread current, long ms, int attempt) {
    synchronized (current) {
      try {
        log.info("Attempt - " + attempt + " Thread." + current.getId()
            + " - " + current.getName() + " is waiting.");
        current.wait(ms);
      } catch (InterruptedException ignored) {
      }
    }
  }
}
