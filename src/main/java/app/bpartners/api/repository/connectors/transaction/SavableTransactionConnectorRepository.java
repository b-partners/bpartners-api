package app.bpartners.api.repository.connectors.transaction;

import app.bpartners.api.model.Money;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.mapper.TransactionMapper;
import app.bpartners.api.repository.jpa.AccountJpaRepository;
import app.bpartners.api.repository.jpa.TransactionJpaRepository;
import app.bpartners.api.repository.jpa.model.HAccount;
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
  private final AccountJpaRepository accountJpaRepository;

  @Override
  public List<TransactionConnector> findByIdAccount(String idAccount) {
    throw new NotImplementedException(UNSUPPORTED_ERROR_MESSAGE);
  }

  //TODO: check why transactions with same bridge ID are persisted twice
  @Override
  public List<TransactionConnector> saveAll(
      String idAccount, List<TransactionConnector> connectors) {
    HAccount account = accountJpaRepository.getById(idAccount);
    List<HTransaction> toSave = connectors.stream()
        .map(connector -> {
          List<HTransaction> bridgeTransactions =
              jpaRepository.findAllByIdBridge(Long.valueOf(connector.getId()));
          if (bridgeTransactions.isEmpty()) {
            String idUser = account.getUser().getId();
            String idBank = account.getIdBank();
            return mapper.toEntity(idAccount, idUser, idBank, connector);
          }
          if (bridgeTransactions.size() > 1) {
            log.warn("Duplicated transactions with same external ID {}",
                describeList(bridgeTransactions));
          }
          HTransaction entity = bridgeTransactions.get(0);
          Money entityMoney = Money.fromMajor(entity.getAmount());
          if (!entityMoney.equals(connector.getAmount())) {
            return entity.toBuilder()
                .amount(connector.getAmount().stringValue())
                .build();
          }
          return entity;
        })
        .collect(Collectors.toList());
    return jpaRepository.saveAll(toSave).stream()
        .map(mapper::toConnector)
        .collect(Collectors.toList());
  }
}
