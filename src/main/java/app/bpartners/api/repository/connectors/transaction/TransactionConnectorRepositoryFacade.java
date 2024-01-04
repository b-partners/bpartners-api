package app.bpartners.api.repository.connectors.transaction;

import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
@AllArgsConstructor
public class TransactionConnectorRepositoryFacade implements TransactionConnectorRepository {
  private final BridgeTransactionConnectorRepository bridgeConnector;

  private TransactionConnectorRepository getTransactionConnectorDependentRep() {
    return new CachedTransactionConnectorRepository(bridgeConnector);
  }

  @Override
  public List<TransactionConnector> findByIdAccount(String idAccount) {
    return getTransactionConnectorDependentRep().findByIdAccount(idAccount);
  }

  @Override
  public List<TransactionConnector> saveAll(
      String idAccount, List<TransactionConnector> transactionConnectors) {
    return getTransactionConnectorDependentRep().saveAll(idAccount, transactionConnectors);
  }
}
