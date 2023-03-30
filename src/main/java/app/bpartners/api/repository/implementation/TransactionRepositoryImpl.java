package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.model.TransactionStatus;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.TransactionMapper;
import app.bpartners.api.repository.TransactionCategoryRepository;
import app.bpartners.api.repository.TransactionRepository;
import app.bpartners.api.repository.bridge.model.Transaction.BridgeTransaction;
import app.bpartners.api.repository.bridge.repository.BridgeTransactionRepository;
import app.bpartners.api.repository.jpa.TransactionJpaRepository;
import app.bpartners.api.repository.jpa.model.HTransaction;
import app.bpartners.api.repository.swan.TransactionSwanRepository;
import app.bpartners.api.repository.swan.model.SwanTransaction;
import app.bpartners.api.repository.swan.model.SwanTransaction.Node;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;

import static app.bpartners.api.model.Transaction.CREDIT_SIDE;
import static app.bpartners.api.model.Transaction.DEBIT_SIDE;
import static app.bpartners.api.model.mapper.TransactionMapper.getStatusFromBridge;
import static app.bpartners.api.model.mapper.TransactionMapper.getTransactionStatus;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Repository
@Slf4j
@AllArgsConstructor
public class TransactionRepositoryImpl implements TransactionRepository {
  private final TransactionSwanRepository swanRepository;
  private final TransactionMapper mapper;
  private final TransactionCategoryRepository categoryRepository;
  private final TransactionJpaRepository jpaRepository;
  private final BridgeTransactionRepository bridgeRepository;
  private final ProjectTokenManager tokenManager;

  @Override
  public List<Transaction> findByAccountId(String accountId) {
    List<SwanTransaction> swanTransactions =
        swanRepository.getByIdAccount(accountId, swanBearerToken());
    if (swanTransactions.isEmpty() && userIsAuthenticated()) {
      List<BridgeTransaction> bridgeTransactions = bridgeRepository.findAuthTransactions();
      if (bridgeTransactions.isEmpty()) {
        List<HTransaction> persistedTransactions = jpaRepository.findAllByIdAccount(accountId);
        return persistedTransactions.stream()
            .map(transaction ->
                mapper.toDomain(transaction,
                    categoryRepository.findByIdTransaction(transaction.getId())))
            .collect(Collectors.toList());
      }
      return bridgeTransactions.stream()
          .map(
              transaction -> {
                HTransaction entity = getUpdatedTransaction(accountId, transaction);
                return mapper.toDomain(transaction, entity,
                    categoryRepository.findByIdTransaction(entity.getId()));
              })
          .collect(Collectors.toList());
    }
    return swanTransactions.stream()
        .map(transaction -> {
          HTransaction entity = getUpdatedTransaction(accountId, transaction);
          return mapper.toDomain(transaction, entity,
              categoryRepository.findByIdTransaction(entity.getId()));
        })
        .collect(Collectors.toUnmodifiableList());
  }


  @Override
  public Transaction findByAccountIdAndId(String accountId, String transactionId) {
    Optional<HTransaction> optionalEntity = jpaRepository.findById(transactionId);
    if (optionalEntity.isEmpty()) {
      throw new NotFoundException("Transaction." + transactionId + " not found.");
    }
    HTransaction persisted = optionalEntity.get();
    return mapper.toDomain(
        swanRepository.findById(persisted.getIdSwan(), swanBearerToken()),
        persisted,
        categoryRepository.findByIdTransaction(transactionId)
    );
  }

  @Override
  public List<Transaction> findByAccountIdAndStatus(String id, TransactionStatus status) {
    return findByAccountId(id).stream()
        .filter(transaction -> transaction.getStatus().equals(status))
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public Transaction save(Transaction toSave) {
    HTransaction entity = jpaRepository.save(mapper.toEntity(toSave));
    SwanTransaction
        swanTransaction = swanRepository.findById(entity.getIdSwan(), swanBearerToken());
    return mapper.toDomain(swanTransaction, entity,
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
    SwanTransaction
        swanTransaction = swanRepository.findById(entity.getIdSwan(), swanBearerToken());
    return mapper.toDomain(swanTransaction, entity,
        categoryRepository.findByIdTransaction(entity.getId()));
  }

  private HTransaction getUpdatedTransaction(
      String accountId,
      SwanTransaction transaction) {
    Optional<HTransaction> optional = jpaRepository.findByIdSwan(transaction.getNode().getId());
    if (optional.isPresent()) {
      HTransaction optionalValue = optional.get();
      checkTransactionUpdates(transaction.getNode(), optionalValue);
      return jpaRepository.save(optionalValue);
    }
    return jpaRepository.save(mapper.toEntity(accountId, transaction));
  }

  private HTransaction getUpdatedTransaction(
      String accountId,
      BridgeTransaction transaction) {
    Optional<HTransaction> optional = jpaRepository.findByIdBridge(transaction.getId());
    if (optional.isPresent()) {
      HTransaction optionalValue = optional.get();
      checkTransactionUpdates(transaction, optionalValue);
      return jpaRepository.save(optionalValue);
    }
    return jpaRepository.save(mapper.toEntity(accountId, transaction));
  }

  private static void checkTransactionUpdates(BridgeTransaction bridgeTransaction,
                                              HTransaction entity) {
    if (entity.getAmount() != null
        && parseFraction(entity.getAmount()).getApproximatedValue() / 100
        != bridgeTransaction.getAmount()) {
      entity.setAmount(
          String.valueOf(parseFraction(bridgeTransaction.getAmount() * 100)));
    }
    if (entity.getCurrency() != null
        && !entity.getCurrency().equals(bridgeTransaction.getCurrency())) {
      entity.setCurrency(bridgeTransaction.getCurrency());
    }
    if (entity.getStatus() != null
        && !entity.getStatus().getValue()
        .equals(getStatusFromBridge(bridgeTransaction).getValue())) {
      entity.setStatus(getStatusFromBridge(bridgeTransaction));
    }
    if (entity.getPaymentDateTime() != null
        && !entity.getPaymentDateTime()
        .equals(bridgeTransaction.getTransactionDate().atStartOfDay(ZoneId.systemDefault())
            .toInstant())) {
      entity.setPaymentDateTime(
          bridgeTransaction.getTransactionDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
    if (entity.getLabel() != null
        && !entity.getLabel().equals((bridgeTransaction.getLabel()))) {
      entity.setLabel(bridgeTransaction.getLabel());
    }
    if (entity.getSide() != null
        && !entity.getSide()
        .equals(bridgeTransaction.getAmount() > 0 ? CREDIT_SIDE : DEBIT_SIDE)) {
      entity.setSide(bridgeTransaction.getAmount() > 0 ? CREDIT_SIDE : DEBIT_SIDE);
    }
  }

  private static void checkTransactionUpdates(Node swanTransaction, HTransaction entity) {
    if (entity.getAmount() != null
        && parseFraction(entity.getAmount()).getApproximatedValue() / 100
        != swanTransaction.getAmount().getValue()) {
      entity.setAmount(
          String.valueOf(parseFraction(swanTransaction.getAmount().getValue() * 100)));
    }
    if (entity.getCurrency() != null
        && !entity.getCurrency().equals(swanTransaction.getAmount().getCurrency())) {
      entity.setCurrency(swanTransaction.getAmount().getCurrency());
    }
    if (entity.getStatus() != null
        && !entity.getStatus().getValue()
        .equals(swanTransaction.getStatusInfo().getStatus())) {
      entity.setStatus(getTransactionStatus(swanTransaction.getStatusInfo().getStatus()));
    }
    if (entity.getPaymentDateTime() != null
        && !entity.getPaymentDateTime().equals(swanTransaction.getCreatedAt())) {
      entity.setPaymentDateTime(swanTransaction.getCreatedAt());
    }
    if (entity.getLabel() != null
        && !entity.getLabel().equals((swanTransaction.getLabel()))) {
      entity.setLabel(swanTransaction.getLabel());
    }
    if (entity.getReference() != null
        && !entity.getReference().equals(swanTransaction.getReference())) {
      entity.setReference(swanTransaction.getReference());
    }
    if (entity.getSide() != null
        && !entity.getSide().equals(swanTransaction.getSide())) {
      entity.setSide(swanTransaction.getSide());
    }
  }

  private boolean userIsAuthenticated() {
    return SecurityContextHolder.getContext().getAuthentication() != null;
  }

  private String swanBearerToken() {
    return userIsAuthenticated() ? AuthProvider.getPrincipal().getBearer() :
        tokenManager.getSwanProjecToken();
  }
}
