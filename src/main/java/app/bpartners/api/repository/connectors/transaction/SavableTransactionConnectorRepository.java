package app.bpartners.api.repository.connectors.transaction;

import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.mapper.TransactionMapper;
import app.bpartners.api.repository.jpa.TransactionJpaRepository;
import app.bpartners.api.repository.jpa.model.HTransaction;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import static app.bpartners.api.service.utils.TransactionUtils.describeList;

@Repository
@AllArgsConstructor
@Slf4j
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
        .map(connector -> {
          List<HTransaction> transactionsWithSameId =
              jpaRepository.findAllByIdBridge(Long.valueOf(connector.getId()));
          if (transactionsWithSameId.isEmpty()) {
            return mapper.toEntity(idAccount, connector);
          }
          if (transactionsWithSameId.size() > 1) {
            log.warn("Duplicated transactions with same external ID {}",
                describeList(transactionsWithSameId));
          }
          return transactionsWithSameId.get(0);
        })
        .collect(Collectors.toList());
    return jpaRepository.saveAll(toSave).stream()
        .map(mapper::toConnector)
        .collect(Collectors.toList());
  }
}
