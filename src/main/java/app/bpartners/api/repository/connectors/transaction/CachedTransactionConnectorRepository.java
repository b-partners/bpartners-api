package app.bpartners.api.repository.connectors.transaction;

import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CachedTransactionConnectorRepository implements TransactionConnectorRepository {
  private final TransactionConnectorRepository toCache;

  @Override
  public List<TransactionConnector> findByIdAccount(String idAccount) {
    return saveAll(idAccount, toCache.findByIdAccount(idAccount));
  }

  @Override
  public List<TransactionConnector> saveAll(
      String idAccount, List<TransactionConnector> transactionConnectors) {
    return toCache.saveAll(idAccount, transactionConnectors);
  }
}
