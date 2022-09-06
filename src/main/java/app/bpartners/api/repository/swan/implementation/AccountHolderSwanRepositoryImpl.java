package app.bpartners.api.repository.swan.implementation;

import app.bpartners.api.repository.swan.AccountHolderSwanRepository;
import app.bpartners.api.repository.swan.SwanApi;
import app.bpartners.api.repository.swan.model.AccountHolder;
import app.bpartners.api.repository.swan.response.AccountHolderResponse;
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
    return List.of(
        swanApi.getData(AccountHolderResponse.class, message).data.accountHolders.edges.get(
            0).node);
  }
}
