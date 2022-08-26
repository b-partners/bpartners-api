package app.bpartners.api.repository.swan.implementation;

import app.bpartners.api.repository.api.swan.SwanApi;
import app.bpartners.api.repository.swan.AccountHolderSwanRepository;
import app.bpartners.api.repository.swan.response.AccountHolderResponse;
import app.bpartners.api.repository.swan.schema.AccountHolder;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class AccountHolderSwanRepositoryImpl implements AccountHolderSwanRepository {
  private static String message =
      "{ \"query\": \"" + "query AccountHolder { accountHolders { edges { node "
          + "{ id info { name } residencyAddress "
          + "{ addressLine1 city country postalCode } } } }}\"}";

  private final SwanApi<AccountHolderResponse> swanApi;

  @Override
  public List<AccountHolder> getAccountHolders() {
    return List.of(swanApi.getData(message, "").data.accountHolders.edges.get(0).node);
  }
}
