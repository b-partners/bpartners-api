package app.bpartners.api.repository.connectors.transaction;

import app.bpartners.api.model.mapper.TransactionMapper;
import app.bpartners.api.service.user.UserService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class BridgeTransactionConnectorRepository implements TransactionConnectorRepository {
  private final SavableTransactionConnectorRepository savableRepository;
  private final TransactionMapper mapper;
  private final UserService userService;

  @Override
  public List<TransactionConnector> findByIdAccount(String idAccount) {
    throw new UnsupportedOperationException("Bridge is now removed !");
  }

  @Override
  public List<TransactionConnector> saveAll(
      String idAccount, List<TransactionConnector> transactionConnectors) {
    return savableRepository.saveAll(idAccount, transactionConnectors);
  }
}
