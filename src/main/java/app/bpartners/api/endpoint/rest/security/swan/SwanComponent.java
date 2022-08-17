package app.bpartners.api.endpoint.rest.security.swan;

import app.bpartners.api.endpoint.rest.model.SwanUser;
import app.bpartners.api.endpoint.rest.model.Token;
import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.ForbiddenException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.repository.mapper.SwanMapper;
import app.bpartners.api.repository.swan.response.TokenResponse;
import app.bpartners.api.repository.swan.response.UserResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SwanComponent {
  private static final String BEARER_PREFIX = "Bearer ";
  private final SwanMapper swanMapper;
  private final SwanConf swanConf;
  private final Principal principal;

  public SwanComponent(SwanMapper swanMapper, SwanConf swanConf) {
    this.swanMapper = swanMapper;
    this.swanConf = swanConf;
    this.principal =
        (Principal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
  }

  public String getSwanUserIdByToken(String accessToken) {
    return getUserByToken(accessToken) != null ? getUserByToken(accessToken).getId() : null;
  }

  public SwanUser getUserByToken(String accessToken) {
    try {
      HttpClient httpClient = HttpClient.newBuilder().build();
      String message = "{\"query\":\"query ProfilePage {user {id firstName lastName " +
          "mobilePhoneNumber identificationStatus idVerified birthDate nationalityCCA3}}\"}";
      HttpRequest request =
          HttpRequest.newBuilder().uri(new URI("https://api.swan.io/sandbox-partner/graphql"))
              .header("Content-Type", "application/json")
              .header("Authorization", BEARER_PREFIX + accessToken)
              .POST(HttpRequest.BodyPublishers.ofString(message)).build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      UserResponse userResponse = new ObjectMapper().findAndRegisterModules() //Load DateTime Module
          .readValue(response.body(), UserResponse.class);
      return swanMapper.graphQLToRest(userResponse.data.user);
    } catch (IOException | InterruptedException | URISyntaxException e) {
      return null;
    }
  }

  public SwanUser whoami() {
    String bearer = principal.getBearer();
    if (bearer == null) {
      throw new ForbiddenException("Access is denied");
    }
    return getUserByToken(principal.getBearer());
  }

  public List<SwanUser> getSwanUsers(PageFromOne page, BoundedPageSize pageSize, String firstName,
                                     String lastName, String mobilePhoneNumber) {
    throw new NotImplementedException("not implemented yet");
  }

  public Token getTokenByCode(String code) {
    try {
      HttpClient httpClient = HttpClient.newBuilder().build();
      String message =
          "client_id=" + swanConf.getClientId() + "&client_secret=" + swanConf.getClientSecret() +
              "&redirect_uri=" + swanConf.getRedirectUri() + "&grant_type=authorization_code" +
              "&code=" + code;
      HttpRequest request = HttpRequest.newBuilder().uri(new URI("/token"))
          .header("Content-Type", "x-www-form-urlencoded")
          .POST(HttpRequest.BodyPublishers.ofString(message)).build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      TokenResponse tokenResponse =
          new ObjectMapper().readValue(response.body(), TokenResponse.class);
      return swanMapper.graphQLToRest(tokenResponse);
    } catch (IOException | InterruptedException | URISyntaxException e) {
      throw new BadRequestException("Invalid code");
    }
  }
}
