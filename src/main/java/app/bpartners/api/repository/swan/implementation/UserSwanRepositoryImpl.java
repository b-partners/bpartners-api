package app.bpartners.api.repository.swan.implementation;

import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.swan.SwanApi;
import app.bpartners.api.repository.swan.UserSwanRepository;
import app.bpartners.api.repository.swan.model.SwanUser;
import app.bpartners.api.repository.swan.response.UserResponse;
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
  private final SwanConf swanConf;
  private static final String QUERY = "{\"query\":\"query ProfilePage "
      + "{user { id firstName lastName mobilePhoneNumber identificationStatus idVerified "
      + "birthDate "
      + "nationalityCCA3}}\"}";

  private final SwanApi<UserResponse> swanApi;

  @Override
  public SwanUser whoami() {
    return swanApi.getData(UserResponse.class, QUERY).getData().getUser();
  }

  @Override
  public SwanUser getByToken(String token) {
    try {
      HttpClient httpClient = HttpClient.newBuilder().build();
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(swanConf.getApiUrl()))
          .header("Content-Type", "application/json")
          .header("Authorization", BEARER_PREFIX + token)
          .POST(HttpRequest.BodyPublishers.ofString(QUERY))
          .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      UserResponse userResponse = new ObjectMapper()
          .findAndRegisterModules() //Load DateTime Module
          .readValue(response.body(), UserResponse.class);
      return userResponse.getData().getUser();
    } catch (IOException | InterruptedException | URISyntaxException e) {
      if (e.getMessage().contains("Token is not active")) {
        throw new BadRequestException("Token is not active");
      }
      return null;
    }
  }
}