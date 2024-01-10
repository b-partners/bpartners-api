package app.bpartners.api.repository;

import static app.bpartners.api.service.utils.TransactionUtils.describeList;

import app.bpartners.api.endpoint.rest.model.TransactionStatus;
import app.bpartners.api.model.JustifyTransaction;
import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.mapper.TransactionMapper;
import app.bpartners.api.model.mapper.TransactionSupportingDocsMapper;
import app.bpartners.api.repository.connectors.transaction.TransactionConnector;
import app.bpartners.api.repository.connectors.transaction.TransactionConnectorRepository;
import app.bpartners.api.repository.jpa.TransactionJpaRepository;
import app.bpartners.api.repository.jpa.TransactionSupportingDocsJpaRepository;
import app.bpartners.api.repository.jpa.model.HTransaction;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
@Slf4j
public class BridgeTransactionRepository implements TransactionRepository {
  private final TransactionMapper mapper;
  private final TransactionCategoryRepository categoryRepository;
  private final TransactionJpaRepository jpaRepository;
  private final TransactionConnectorRepository connectorRepository;
  private final TransactionSupportingDocsJpaRepository transactionDocsRep;
  private final TransactionSupportingDocsMapper transactionDocsMapper;

  @Override
  public List<Transaction> findByIdAccount(
      String idAccount, String title, TransactionStatus status, int page, int pageSize) {
    throw new NotImplementedException("Not supported ! Must be pageable");
  }

  @Override
  public List<Transaction> findByAccountId(String id) {
    List<TransactionConnector> connectors = connectorRepository.findByIdAccount(id);
    List<HTransaction> entities =
        connectors.stream()
            .map(
                connector -> {
                  List<HTransaction> bridgeTransactions =
                      jpaRepository.findAllByIdBridge(Long.valueOf(connector.getId()));
                  if (bridgeTransactions.isEmpty()) {
                    throw new NotFoundException(
                        "Transaction(externalId=" + connector.getId() + ") not found");
                  }
                  if (bridgeTransactions.size() > 1) {
                    log.warn(
                        "Duplicated transactions with same external ID {}",
                        describeList(bridgeTransactions));
                  }
                  return bridgeTransactions.get(0);
                })
            .toList();
    return entities.stream()
        .map(
            entity ->
                mapper.toDomain(
                    entity,
                    categoryRepository.findByIdTransaction(entity.getId()),
                    transactionDocsRep.findAllByIdTransaction(entity.getId()).stream()
                        .map(transactionDocsMapper::toDomain)
                        .toList()))
        // TODO: when getting from database only, sort by payment date DESC directly in db query
        .sorted(Comparator.comparing(Transaction::getPaymentDatetime).reversed())
        .collect(Collectors.toList());
  }

  @Override
  public Transaction findById(String id) {
    throw new NotFoundException("Not supported ! Must be pageable");
  }

  @Override
  public List<Transaction> findByAccountIdAndStatus(String id, TransactionStatus status) {
    return findByAccountId(id).stream()
        .filter(transaction -> transaction.getStatus().equals(status))
        .toList();
  }

  @Override
  public Transaction save(JustifyTransaction justifyTransaction) {
    throw new NotImplementedException("Not supported !");
  }

  @Override
  public List<Transaction> saveAll(List<Transaction> transactions) {
    throw new NotImplementedException("Not supported !");
  }

  @Override
  public List<Transaction> findByAccountIdAndStatusBetweenInstants(
      String id, TransactionStatus status, Instant from, Instant to) {
    return findByAccountIdAndStatus(id, status).stream()
        .filter(
            transaction ->
                transaction.getPaymentDatetime().isAfter(from)
                    && transaction.getPaymentDatetime().isBefore(to))
        .toList();
  }

  @Override
  public Transaction getById(String idTransaction) {
    throw new NotImplementedException("Not supported !");
  }

  @Override
  public void removeAll(List<Transaction> toRemove) {
    throw new NotImplementedException("Not supported !");
  }
}
