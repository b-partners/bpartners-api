package app.bpartners.api.repository.connectors.transaction;

import static app.bpartners.api.endpoint.rest.security.AuthProvider.userIsAuthenticated;

import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.model.UserToken;
import app.bpartners.api.model.mapper.TransactionMapper;
import app.bpartners.api.repository.bridge.repository.BridgeTransactionRepository;
import app.bpartners.api.service.UserService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class BridgeTransactionConnectorRepository implements TransactionConnectorRepository {
  private final SavableTransactionConnectorRepository savableRepository;
  private final BridgeTransactionRepository bridgeRepository;
  private final TransactionMapper mapper;
  private final UserService userService;

  @Override
  public List<TransactionConnector> findByIdAccount(String idAccount) {
    String bearerFromAccount =
        userIsAuthenticated() ? AuthProvider.getBearer() : bridgeAccessToken(idAccount);
    return bridgeRepository.findByBearer(bearerFromAccount).stream()
        .map(mapper::toConnector)
        .collect(Collectors.toList());
  }

  @Override
  public List<TransactionConnector> saveAll(
      String idAccount, List<TransactionConnector> transactionConnectors) {
    return savableRepository.saveAll(idAccount, transactionConnectors);
  }

  private String bridgeAccessToken(String idAccount) {
    UserToken userToken = userService.getLatestTokenByAccount(idAccount);
    return userToken == null ? null : userToken.getAccessToken();
  }
}
