package app.bpartners.api.repository.swan.implementation;

import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.swan.AccountHolderSwanRepository;
import app.bpartners.api.repository.swan.SwanApi;
import app.bpartners.api.repository.swan.SwanCustomApi;
import app.bpartners.api.repository.swan.model.AccountHolder;
import app.bpartners.api.repository.swan.response.AccountHolderResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

/*Note that there is security check in security layer, so that these requests are allowed only when
 *accountId match with authenticated account_id
 */

@Repository
@AllArgsConstructor
public class AccountHolderSwanRepositoryImpl implements AccountHolderSwanRepository {

  private final SwanApi<AccountHolderResponse> swanApi;
  private final SwanCustomApi<AccountHolderResponse> swanCustomApi;
  private static final String QUERY =
      "{ \"query\": \"" + "query AccountHolder { accountHolders { edges { node "
          + "{ id  info { ... on AccountHolderCompanyInfo { name registrationNumber "
          + "businessActivity businessActivityDescription }} residencyAddress "
          + "{ addressLine1 city country postalCode } verificationStatus } } }}\"}";

  @Override
  public List<AccountHolder> findAllByAccountId(String accountId) {
    return List.of(
        swanApi.getData(AccountHolderResponse.class, QUERY).getData().getAccountHolders()
            .getEdges().get(0).getNode());
  }

  @Override
  public List<AccountHolder> findAllByBearerAndAccountId(String bearer, String accountId) {
    return List.of(
        swanCustomApi.getData(AccountHolderResponse.class, QUERY, bearer).getData()
            .getAccountHolders()
            .getEdges().get(0).getNode());
  }

  @Override
  public AccountHolder getById(String id) {
    AccountHolder accountHolder =
        swanApi.getData(AccountHolderResponse.class, QUERY).getData().getAccountHolders()
            .getEdges().get(0).getNode();
    if (!accountHolder.getId().equals(id)) {
      throw new NotFoundException("AccountHolder." + id + " not found");
    }
    return accountHolder;
  }
}
