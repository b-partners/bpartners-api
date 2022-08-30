package app.bpartners.api.repository.swan.implementation;

import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.endpoint.rest.security.principal.PrincipalProvider;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.repository.swan.UserSwanRepository;
import app.bpartners.api.repository.swan.response.UserResponse;
import app.bpartners.api.repository.swan.model.SwanUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import static app.bpartners.api.endpoint.rest.security.swan.SwanConf.BEARER_PREFIX;

@Repository
@AllArgsConstructor
public class UserSwanRepositoryImpl implements UserSwanRepository {
  private final PrincipalProvider auth;
  private SwanConf swanConf;

  @Override
  public SwanUser whoami() {
    try {
      HttpClient httpClient = HttpClient.newBuilder().build();
      String message = "{\"query\":\"query ProfilePage "
          + "{user { id firstName lastName mobilePhoneNumber identificationStatus idVerified "
          + "birthDate "
          + "nationalityCCA3}}\"}";
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(swanConf.getApiUrl()))
          .header("Content-Type", "application/json")
          .header("Authorization", BEARER_PREFIX + bearerToken())
          .POST(HttpRequest.BodyPublishers.ofString(message))
          .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      UserResponse userResponse = new ObjectMapper()
          .findAndRegisterModules() //Load DateTime Module
          .readValue(response.body(), UserResponse.class);
      return userResponse.data.user;
    } catch (IOException | InterruptedException | URISyntaxException e) {
      return null;
    }
  }

  private String bearerToken() {
    return ((Principal) auth.getAuthentication().getPrincipal()).getBearer();
  }
}