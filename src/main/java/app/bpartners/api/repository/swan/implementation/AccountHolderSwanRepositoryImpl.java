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


  private final SwanApi<AccountHolderResponse> swanApi;
  private static final String QUERY =
      "{ \"query\": \"" + "query AccountHolder { accountHolders { edges { node "
          + "{ id info { name } residencyAddress "
          + "{ addressLine1 city country postalCode } } } }}\"}";

  @Override
  public List<AccountHolder> getAccountHolders() {
    return List.of(
        swanApi.getData(AccountHolderResponse.class, QUERY).getData().getAccountHolders().getEdges()
            .get(0).getNode());
  }
}
