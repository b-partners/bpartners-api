package app.bpartners.api.repository.swan.implementation;

import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.AccountMapper;
import app.bpartners.api.repository.AccountConnectorRepository;
import app.bpartners.api.repository.implementation.SavableAccountConnectorRepository;
import app.bpartners.api.repository.model.AccountConnector;
import app.bpartners.api.repository.swan.SwanApi;
import app.bpartners.api.repository.swan.SwanCustomApi;
import app.bpartners.api.repository.swan.model.SwanAccount;
import app.bpartners.api.repository.swan.response.AccountResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import static app.bpartners.api.endpoint.rest.security.AuthProvider.getAuthenticatedUserId;
import static app.bpartners.api.endpoint.rest.security.AuthProvider.userIsAuthenticated;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.util.stream.Collectors.toUnmodifiableList;

//TODO: add unit test
@Repository
@AllArgsConstructor
public class SwanAccountConnectorRepository implements AccountConnectorRepository {
  private final SavableAccountConnectorRepository savableRepository;
  private final SwanApi<AccountResponse> swanApi;
  private final SwanCustomApi<AccountResponse> swanCustomApi;
  private final AccountMapper accountMapper;
  private static final String QUERY =
      "{\"query\": \"query Account { accounts { edges { node { "
          + "id name IBAN BIC balances { available { value } } statusInfo {status} } } } } \"}";

  @Override
  public List<AccountConnector> findByBearer(String bearer) {
    AccountResponse data = swanCustomApi.getData(AccountResponse.class, QUERY, bearer);
    return data == null ? List.of() : data.getData().getAccounts().getEdges().stream()
        .map(edge -> accountMapper.toConnector(edge.getNode()))
        .collect(toUnmodifiableList());
  }

  @Override
  public AccountConnector findById(String accountId) {
    AccountResponse data = swanApi.getData(AccountResponse.class, QUERY);
    List<SwanAccount> accounts =
        data == null ? List.of() : data.getData().getAccounts().getEdges().stream()
            .map(AccountResponse.Edge::getNode)
            .collect(toUnmodifiableList());
    if (accounts.isEmpty()) {
      throw new NotFoundException("No swan account found for id: " + accountId);
    }
    if (accounts.size() > 1) {
      throw new ApiException(SERVER_EXCEPTION,
          "Only one account expected but received: " + accounts);
    }
    return accountMapper.toConnector(accounts.get(0));
  }


  @Override
  public List<AccountConnector> findByUserId(String userId) {
    if (!userIsAuthenticated() || getAuthenticatedUserId() == null
        || !getAuthenticatedUserId().equals(userId)) {
      return List.of();
    }
    return findByBearer(AuthProvider.getBearer());
  }

  @Override
  public AccountConnector save(String idUser, AccountConnector accountConnector) {
    return savableRepository.save(idUser, accountConnector);
  }

  @Override
  public List<AccountConnector> saveAll(String idUser, List<AccountConnector> accountConnectors) {
    return savableRepository.saveAll(idUser, accountConnectors);
  }
}
