package app.bpartners.api.repository.connectors.transaction;

import java.util.List;

public interface TransactionConnectorRepository {
  List<TransactionConnector> findByIdAccount(String idAccount);

  List<TransactionConnector> saveAll(
      String idAccount, List<TransactionConnector> transactionConnectors);
}
