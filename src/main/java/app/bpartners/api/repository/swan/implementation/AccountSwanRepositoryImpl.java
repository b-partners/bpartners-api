package app.bpartners.api.repository.swan.implementation;

import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.endpoint.rest.security.principal.PrincipalProvider;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.repository.swan.AccountSwanRepository;
import app.bpartners.api.repository.swan.response.AccountResponse;
import app.bpartners.api.repository.swan.schema.SwanAccount;
import app.bpartners.api.utils.ReflectApi;
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
public class AccountSwanRepositoryImpl implements AccountSwanRepository {
  private final PrincipalProvider auth;

  private final SwanConf swanConf;

  @Override
  public SwanAccount getAccount() {
    try {
      HttpClient httpClient = HttpClient.newBuilder().build();
      String message = "{\"query\":\"query AccountPage "
          +ReflectApi.getFields("account",SwanAccount.class)+"\"}";
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(swanConf.getApiUrl()))
          .header("Content-Type", "application/json")
          .header("Authorization", BEARER_PREFIX + bearerToken())
          .POST(HttpRequest.BodyPublishers.ofString(message))
          .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      AccountResponse userResponse = new ObjectMapper()
          .findAndRegisterModules() //Load DateTime Module
          .readValue(response.body(), AccountResponse.class);
      return userResponse.data.account;
    } catch (IOException | InterruptedException | URISyntaxException e) {
      return null;
    }
  }

  private String bearerToken() {
    return ((Principal) auth.getAuthentication().getPrincipal()).getBearer();
  }
}
