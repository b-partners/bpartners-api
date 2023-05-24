package app.bpartners.api.repository.connectors.transaction;

import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.mapper.TransactionMapper;
import app.bpartners.api.repository.jpa.TransactionJpaRepository;
import app.bpartners.api.repository.jpa.model.HTransaction;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@AllArgsConstructor
public class SavableTransactionConnectorRepository
    implements TransactionConnectorRepository {
  private static final String UNSUPPORTED_ERROR_MESSAGE = "Unsupported: only saving methods are!";
  private final TransactionJpaRepository jpaRepository;
  private final TransactionMapper mapper;

  @Override
  public List<TransactionConnector> findByIdAccount(String idAccount) {
    throw new NotImplementedException(UNSUPPORTED_ERROR_MESSAGE);
  }

  @Override
  public List<TransactionConnector> saveAll(
      String idAccount, List<TransactionConnector> connectors) {
    List<HTransaction> toSave = connectors.stream()
        .map(connector -> jpaRepository.findByIdBridge(
                Long.valueOf(connector.getId()))
            .orElse(mapper.toEntity(idAccount, connector)))
        .collect(Collectors.toList());
    return jpaRepository.saveAll(toSave).stream()
        .map(mapper::toConnector)
        .collect(Collectors.toList());
  }
}
