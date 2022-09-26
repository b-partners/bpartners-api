package app.bpartners.api.repository.swan.implementation;

import app.bpartners.api.repository.swan.AccountSwanRepository;
import app.bpartners.api.repository.swan.SwanApi;
import app.bpartners.api.repository.swan.SwanCustomApi;
import app.bpartners.api.repository.swan.model.SwanAccount;
import app.bpartners.api.repository.swan.response.AccountResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class AccountSwanRepositoryImpl implements AccountSwanRepository {
  private static final String QUERY =
      "{\"query\": \"query Account { accounts { edges { node { id name IBAN BIC } } } } \"}";
  private final SwanApi<AccountResponse> swanApi;
  private final SwanCustomApi<AccountResponse> swanCustomApi;

  @Override
  public List<SwanAccount> findAll() {
    return List.of(
        swanApi.getData(AccountResponse.class, QUERY).getData().getAccounts().getEdges().get(0)
            .getNode());
  }

  @Override
  public List<SwanAccount> findByBearer(String bearer) {
    return List.of(
        swanCustomApi.getData(AccountResponse.class, QUERY, bearer)
            .getData().getAccounts().getEdges().get(0)
            .getNode());
  }

  @Override
  public List<SwanAccount> findById(String id) {
    return List.of(
        swanApi.getData(AccountResponse.class, QUERY).getData().getAccounts().getEdges().get(0)
            .getNode());
  }
}
