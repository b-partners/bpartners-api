package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.model.TransactionStatus;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.model.JustifyTransaction;
import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.UserToken;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.TransactionMapper;
import app.bpartners.api.repository.TransactionCategoryRepository;
import app.bpartners.api.repository.TransactionRepository;
import app.bpartners.api.repository.bridge.model.Transaction.BridgeTransaction;
import app.bpartners.api.repository.bridge.repository.BridgeTransactionRepository;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.TransactionJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.repository.jpa.model.HTransaction;
import app.bpartners.api.service.UserService;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import static app.bpartners.api.endpoint.rest.security.AuthProvider.userIsAuthenticated;

@Repository
@Slf4j
@AllArgsConstructor
public class TransactionRepositoryImpl implements TransactionRepository {
  private final TransactionMapper mapper;
  private final TransactionCategoryRepository categoryRepository;
  private final TransactionJpaRepository jpaRepository;
  private final BridgeTransactionRepository bridgeRepository;
  private final UserService userService;
  private final InvoiceJpaRepository invoiceJpaRepository;

  @Override
  public List<Transaction> findByAccountId(String accountId) {
    List<BridgeTransaction> bridgeTransactions =
        userIsAuthenticated()
            ? bridgeRepository.findByBearer(AuthProvider.getBearer())
            : bridgeRepository.findByBearer(bridgeAccessToken(accountId));
    if (bridgeTransactions.isEmpty()) {
      return List.of();
    }
    return bridgeTransactions.stream()
        .map(
            transaction -> {
              HTransaction entity = getUpdatedTransaction(accountId, transaction);
              return mapper.toDomain(transaction, entity,
                  categoryRepository.findByIdTransaction(entity.getId()));
            })
        .sorted(Comparator.comparing(Transaction::getPaymentDatetime).reversed())
        .collect(Collectors.toList());
  }

  @Override
  public Transaction findById(String id) {
    HTransaction transaction = jpaRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Transaction." + id + " is not found."));
    return mapper.toDomain(
        transaction, categoryRepository.findByIdTransaction(transaction.getId()));
  }

  @Override
  public List<Transaction> findByAccountIdAndStatus(String id, TransactionStatus status) {
    return findByAccountId(id).stream()
        .filter(transaction -> transaction.getStatus().equals(status))
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public Transaction save(JustifyTransaction toSave) {
    HTransaction entity = jpaRepository.findById(toSave.getIdTransaction())
        .orElseThrow(() -> new NotFoundException("Transaction(id=" + toSave.getIdTransaction()
            + ") not found"));
    HInvoice invoice = invoiceJpaRepository.findById(toSave.getIdInvoice())
        .orElseThrow(() -> new NotFoundException("Invoice(id=" + toSave.getIdInvoice()
            + ") not found"));
    HTransaction savedTransaction = jpaRepository.save(entity.toBuilder()
        .invoice(invoice)
        .build());
    return mapper.toDomain(savedTransaction,
        categoryRepository.findByIdTransaction(entity.getId()));
  }

  @Override
  public List<Transaction> findByAccountIdAndStatusBetweenInstants(
      String id, TransactionStatus status,
      Instant from, Instant to) {
    return findByAccountIdAndStatus(id, status).stream()
        .filter(
            transaction -> transaction.getPaymentDatetime().isAfter(from)
                &&
                transaction.getPaymentDatetime().isBefore(to)
        )
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public Transaction getById(String idTransaction) {
    HTransaction entity = jpaRepository.findById(idTransaction)
        .orElseThrow(() -> new NotFoundException(
            "Transaction." + idTransaction + " not found"));
    return mapper.toDomain(entity, categoryRepository.findByIdTransaction(entity.getId()));
  }

  private HTransaction getUpdatedTransaction(
      String accountId, BridgeTransaction transaction) {
    HTransaction transactionEntity = mapper.toEntity(accountId, transaction);
    jpaRepository.findByIdBridge(transaction.getId())
        .ifPresent(entity -> transactionEntity.setId(entity.getId()));
    return jpaRepository.save(transactionEntity);
  }

  private String bridgeAccessToken(String accountId) {
    UserToken userToken = userService.getLatestTokenByAccount(accountId);
    return userToken == null ? null : userToken.getAccessToken();
  }
}
