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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import static app.bpartners.api.endpoint.rest.security.swan.SwanConf.SWAN_TOKEN_URL;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.service.utils.URLUtils.URLEncodeMap;

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
  private final SwanCustomApi<T> swanCustomApi;
  private final SwanConf swanConf;

  public T getData(Class<T> genericClass, String message) {
    return swanCustomApi.getData(genericClass, message, bearerToken());
  }

  private String bearerToken() {
    return ((Principal) auth.getAuthentication().getPrincipal()).getBearer();
  }

  public ProjectTokenResponse getProjectToken() {
    try {
      HttpClient httpClient = HttpClient.newBuilder().build();
      HttpRequest request = HttpRequest.newBuilder().uri(new URI(SWAN_TOKEN_URL))
          .header("Content-Type", "application/x-www-form-urlencoded")
          .POST(HttpRequest.BodyPublishers.ofString(URLEncodeMap(swanConf.getParams()))).build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      return new ObjectMapper().findAndRegisterModules()//Load DateTime Module
          .readValue(response.body(), ProjectTokenResponse.class);
    } catch (IOException | URISyntaxException e) {
      throw new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }
  /*
    TODO: if getProjectToken fails you should get a new Token
   */
}
