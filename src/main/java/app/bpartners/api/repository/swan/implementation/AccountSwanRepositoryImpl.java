package app.bpartners.api.repository.swan.implementation;

import app.bpartners.api.repository.api.swan.SwanApi;
import app.bpartners.api.repository.swan.AccountSwanRepository;
import app.bpartners.api.repository.swan.response.AccountResponse;
import app.bpartners.api.repository.swan.schema.SwanAccount;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class AccountSwanRepositoryImpl implements AccountSwanRepository {
  private static String message =
      "{\"query\": \"query Account { accounts { edges { node { id name IBAN BIC } } } } \"}";
  private final SwanApi<AccountResponse> swanApi;

  @Override
  public List<SwanAccount> getAccounts() {
    SwanAccount account =
        swanApi.getData(message, "").getData().getAccounts().getEdges().get(0).getNode();
    return List.of(account);
  }
}
