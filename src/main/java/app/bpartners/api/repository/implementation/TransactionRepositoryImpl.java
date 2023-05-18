package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.model.TransactionStatus;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.TransactionCategory;
import app.bpartners.api.model.UserToken;
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
import app.bpartners.api.service.UserService;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import static app.bpartners.api.endpoint.rest.security.AuthProvider.userIsAuthenticated;
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
  private final UserService userService;

  @Override
  public List<Transaction> findByAccountId(String accountId) {
    List<SwanTransaction> swanTransactions =
        swanRepository.getByIdAccount(accountId, swanUserToken());
    if (swanTransactions.isEmpty()) {
      List<BridgeTransaction> bridgeTransactions =
          userIsAuthenticated()
              ? bridgeRepository.findByBearer(AuthProvider.getBearer())
              : bridgeRepository.findByBearer(bridgeAccessToken(accountId));
      if (bridgeTransactions.isEmpty()) {
        return List.of(); //No transactions neither bridge nor swan return transactions
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
    return swanTransactions.stream()
        .map(transaction -> {
          HTransaction entity = getUpdatedTransaction(accountId, transaction);
          return mapper.toDomain(transaction, entity,
              categoryRepository.findByIdTransaction(entity.getId()));
        })
        .sorted(Comparator.comparing(Transaction::getPaymentDatetime).reversed())
        .collect(Collectors.toUnmodifiableList());
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
    SwanTransaction swanTransaction =
        swanRepository.findById(entity.getIdSwan(), swanUserToken());
    BridgeTransaction bridgeTransaction =
        bridgeRepository.findById(entity.getIdBridge());
    TransactionCategory category = categoryRepository.findByIdTransaction(entity.getId());
    return swanTransaction == null
        ? mapper.toDomain(bridgeTransaction, entity, category)
        : mapper.toDomain(swanTransaction, entity, category);
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
    SwanTransaction swanTransaction =
        swanRepository.findById(entity.getIdSwan(), swanUserToken());
    BridgeTransaction bridgeTransaction =
        bridgeRepository.findById(entity.getIdBridge());
    TransactionCategory category = categoryRepository.findByIdTransaction(entity.getId());
    return swanTransaction == null
        ? mapper.toDomain(bridgeTransaction, entity, category)
        : mapper.toDomain(swanTransaction, entity, category);
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
      String accountId, BridgeTransaction transaction) {
    HTransaction transactionEntity = mapper.toEntity(accountId, transaction);
    jpaRepository.findByIdBridge(transaction.getId())
        .ifPresent(entity -> transactionEntity.setId(entity.getId()));
    return jpaRepository.save(transactionEntity);
  }

  private static void checkTransactionUpdates(Node swanTransaction, HTransaction entity) {
    if (entity.getAmount() == null
        || (entity.getAmount() != null
        && parseFraction(entity.getAmount()).getApproximatedValue() / 100
        != swanTransaction.getAmount().getValue())) {
      entity.setAmount(
          String.valueOf(parseFraction(swanTransaction.getAmount().getValue() * 100)));
    }
    if (entity.getCurrency() == null
        || (entity.getCurrency() != null
        && !entity.getCurrency().equals(swanTransaction.getAmount().getCurrency()))) {
      entity.setCurrency(swanTransaction.getAmount().getCurrency());
    }
    if (entity.getStatus() == null
        || (entity.getStatus() != null
        && !entity.getStatus().getValue()
        .equals(swanTransaction.getStatusInfo().getStatus()))) {
      entity.setStatus(getTransactionStatus(swanTransaction.getStatusInfo().getStatus()));
    }
    if (entity.getPaymentDateTime() == null
        || (entity.getPaymentDateTime() != null
        && !entity.getPaymentDateTime().equals(swanTransaction.getCreatedAt()))) {
      entity.setPaymentDateTime(swanTransaction.getCreatedAt());
    }
    if (entity.getLabel() == null
        || (entity.getLabel() != null
        && !entity.getLabel().equals((swanTransaction.getLabel())))) {
      entity.setLabel(swanTransaction.getLabel());
    }
    if (entity.getReference() == null
        || (entity.getReference() != null
        && !entity.getReference().equals(swanTransaction.getReference()))) {
      entity.setReference(swanTransaction.getReference());
    }
    if (entity.getSide() == null
        || (entity.getSide() != null
        && !entity.getSide().equals(swanTransaction.getSide()))) {
      entity.setSide(swanTransaction.getSide());
    }
  }

  private String swanUserToken() {
    return userIsAuthenticated() ? AuthProvider.getBearer() : null;
  }

  private String bridgeAccessToken(String accountId) {
    UserToken userToken = userService.getLatestTokenByAccount(accountId);
    return userToken == null ? null : userToken.getAccessToken();
  }
}
