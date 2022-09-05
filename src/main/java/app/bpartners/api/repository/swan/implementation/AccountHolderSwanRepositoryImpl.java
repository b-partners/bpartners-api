package app.bpartners.api.repository.swan.implementation;

import app.bpartners.api.repository.swan.AccountHolderSwanRepository;
import app.bpartners.api.repository.swan.SwanApi;
import app.bpartners.api.repository.swan.model.SwanAccountHolder;
import app.bpartners.api.repository.swan.response.AccountHolderResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class AccountHolderSwanRepositoryImpl implements AccountHolderSwanRepository {
  private static final String query =
      "{ \"query\": \"" + "query AccountHolder { accountHolders { edges { node "
          + "{ id info { name } residencyAddress "
          + "{ addressLine1 city country postalCode } } } }}\"}";

  private final SwanApi<AccountHolderResponse> swanApi;

  @Override
  public List<SwanAccountHolder> getAccountHolders() {
    try {
      return List.of(swanApi.getData(AccountHolderResponse.class, query,
          null).data.accountHolders.edges.get(0).node);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}
