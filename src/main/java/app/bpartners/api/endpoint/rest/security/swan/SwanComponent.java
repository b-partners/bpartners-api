package app.bpartners.api.endpoint.rest.security.swan;

import app.bpartners.api.endpoint.rest.model.OauthToken;
import app.bpartners.api.endpoint.rest.model.SwanUser;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.NotImplementedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.stereotype.Component;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

@Component
public class SwanComponent {

  public static OauthToken getTokenByCode(String code) {
    HttpClient authorizedClient = HttpClient.newBuilder().build();
    String basePath = SwanConf.getTokenProviderUrl();
    try {
      HttpResponse<String> response = authorizedClient.send(
          HttpRequest.newBuilder()
              .uri(URI.create(basePath))
              .header("Content-type", "application/x-www-form-urlencoded")
              .POST(HttpRequest.BodyPublishers
                  .ofString(
                      String.format("code=%s&client_id=%s&client_secret=%s&redirect_uri=%s"
                              + "&grant_type=authorization_code", code, SwanConf.getClientId(),
                          SwanConf.getClientSecret(), SwanConf.getRedirectUri())))
              .build(),
          HttpResponse.BodyHandlers.ofString());
      return new ObjectMapper().readValue(response.body(), OauthToken.class);
    } catch (IOException | InterruptedException e) {
      throw new ApiException(SERVER_EXCEPTION, e.getMessage());
    }
  }

  public static SwanUser getUserById(String swanUser1Id) {
    throw new NotImplementedException("GraphQL API client not yet configured");
  }
}
