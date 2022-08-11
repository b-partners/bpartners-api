package app.bpartners.api.endpoint.rest.security.swan;

import app.bpartners.api.endpoint.rest.model.SwanUser;
import app.bpartners.api.endpoint.rest.model.Token;
import app.bpartners.api.graphql.SwanMapper;
import app.bpartners.api.graphql.responses.TokenResponse;
import app.bpartners.api.graphql.responses.UserResponse;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotImplementedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class SwanComponent {
  private final SwanMapper swanMapper;

  private final SwanConf swanConf;
  private static final String BEARER_PREFIX = "Bearer ";

  public String getSwanUserIdByToken(String accessToken) {
    return getUserInfos(accessToken) != null ? getUserInfos(accessToken).getId() : null;
  }

  public SwanUser getUserById(String swanUserId) {
    throw new NotImplementedException("GraphQL API client not yet configured");
  }

  public SwanUser getUserInfos(String accessToken) {
    try {
      HttpClient httpClient = HttpClient.newBuilder().build();
      String message = "{\"query\":\"query ProfilePage {user {id firstName lastName "
          + "mobilePhoneNumber identificationStatus idVerified birthDate nationalityCCA3}}\"}";
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI("https://api.swan.io/sandbox-partner/graphql"))
          .header("Content-Type", "application/json")
          .header("Authorization", BEARER_PREFIX + accessToken)
          .POST(HttpRequest.BodyPublishers.ofString(message))
          .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      UserResponse userResponse = new ObjectMapper()
          .findAndRegisterModules() //Load DateTime Module
          .readValue(response.body(), UserResponse.class);
      return swanMapper.graphQLToRest(userResponse.data.user);
    } catch (IOException | InterruptedException | URISyntaxException e) {
      return null;
    }
  }

  public Token getTokenByCode(String code) {
    try {
      HttpClient httpClient = HttpClient.newBuilder().build();
      String message =
          "client_id=" + swanConf.getClientId()
              + "&client_secret=" + swanConf.getClientSecret()
              + "&redirect_uri=" + swanConf.getRedirectUri()
              + "&grant_type=authorization_code"
              + "&code=" + code;
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI("https://oauth.swan.io/oauth2/token"))
          .header("Content-Type", "x-www-form-urlencoded")
          .POST(HttpRequest.BodyPublishers.ofString(message))
          .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      TokenResponse tokenResponse = new ObjectMapper().readValue(response.body(),
          TokenResponse.class);
      return swanMapper.graphQLToRest(tokenResponse);
    } catch (IOException | InterruptedException | URISyntaxException e) {
      throw new BadRequestException("Invalid code");
    }
  }
}
