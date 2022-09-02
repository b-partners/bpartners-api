package app.bpartners.api.repository.swan;

import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.endpoint.rest.security.principal.PrincipalProvider;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.model.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javax.annotation.Nullable;
import org.springframework.stereotype.Component;

import static app.bpartners.api.endpoint.rest.security.swan.SwanConf.BEARER_PREFIX;

/*
 *
 * T generic type for SWAN GraphQL model
 * ex : SwanAccountHolder or SwanAccount
 *
 * */

@Component
public class SwanApi<T> {
  private final PrincipalProvider auth;
  private SwanConf swanConf;

  public SwanApi(PrincipalProvider auth, SwanConf swanConf) {
    this.auth = auth;
    this.swanConf = swanConf;
  }

  public T getData(String className, String query, @Nullable String customToken)
      throws ClassNotFoundException {
    String token = bearerToken();
    Class<T> objectClass = (Class<T>) Class.forName(className);

    if (customToken != null) {
      token = customToken;
    }
    try {
      HttpClient httpClient = HttpClient.newBuilder().build();
      HttpRequest request = HttpRequest.newBuilder().uri(new URI(swanConf.getApiUrl()))
          .header("Content-Type", "application/json")
          .header("Authorization", BEARER_PREFIX + token)
          .POST(HttpRequest.BodyPublishers.ofString(query)).build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      return new ObjectMapper().readValue(response.body(), objectClass);
    } catch (IOException | InterruptedException | URISyntaxException e) {
      throw new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, e);
    }
  }

  public String bearerToken() {
    return ((Principal) auth.getAuthentication().getPrincipal()).getBearer();
  }
}
