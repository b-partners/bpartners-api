package app.bpartners.api.repository.swan.implementation;

import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.endpoint.rest.security.principal.PrincipalProvider;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.swan.AccountSwanRepository;
import app.bpartners.api.repository.swan.response.AccountResponse;
import app.bpartners.api.repository.swan.schema.SwanAccount;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import static app.bpartners.api.endpoint.rest.security.swan.SwanConf.BEARER_PREFIX;

@Repository
@AllArgsConstructor
public class AccountSwanRepositoryImpl implements AccountSwanRepository {
  private final PrincipalProvider auth;
  private SwanConf swanConf;

  @Override
  public List<SwanAccount> getAccounts() {
    try {
      HttpClient httpClient = HttpClient.newBuilder().build();
      String message =
          "{\"query\": \"query Account { accounts { edges { node { id name IBAN BIC } } } } \"}";
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(swanConf.getApiUrl()))
          .header("Content-Type", "application/json")
          .header("Authorization", BEARER_PREFIX + bearerToken())
          .POST(HttpRequest.BodyPublishers.ofString(message))
          .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      AccountResponse accountResponse = new ObjectMapper()
          .findAndRegisterModules()
          .readValue(response.body(), AccountResponse.class);
      SwanAccount account = accountResponse.data.accounts.edges.get(0).node;
      return List.of(account);
    } catch (IOException | InterruptedException | URISyntaxException e) {
      throw new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, e);
    }
  }

  private String bearerToken() {
    return ((Principal) auth.getAuthentication().getPrincipal()).getBearer();
  }
}
