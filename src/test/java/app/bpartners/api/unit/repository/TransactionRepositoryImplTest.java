package app.bpartners.api.unit.repository;

import app.bpartners.api.endpoint.rest.model.TransactionStatus;
import app.bpartners.api.endpoint.rest.security.AuthenticatedResourceProvider;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.TransactionCategory;
import app.bpartners.api.model.mapper.TransactionMapper;
import app.bpartners.api.repository.TransactionCategoryRepository;
import app.bpartners.api.repository.implementation.TransactionRepositoryImpl;
import app.bpartners.api.repository.jpa.TransactionJpaRepository;
import app.bpartners.api.repository.jpa.model.HTransaction;
import app.bpartners.api.repository.swan.TransactionSwanRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.swanTransaction1;
import static app.bpartners.api.integration.conf.TestUtils.swanTransaction2;
import static app.bpartners.api.integration.conf.TestUtils.swanTransaction3;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TransactionRepositoryImplTest {
  private TransactionRepositoryImpl transactionRepositoryImpl;
  private TransactionSwanRepository swanRepository;
  private TransactionMapper mapper;
  private TransactionCategoryRepository categoryRepository;
  private AuthenticatedResourceProvider provider;
  private TransactionJpaRepository jpaRepository;

  @BeforeEach
  void setUp() {
    swanRepository = mock(TransactionSwanRepository.class);
    provider = mock(AuthenticatedResourceProvider.class);
    mapper = new TransactionMapper(provider);
    categoryRepository = mock(TransactionCategoryRepository.class);
    jpaRepository = mock(TransactionJpaRepository.class);
    transactionRepositoryImpl = new TransactionRepositoryImpl(swanRepository, mapper,
        categoryRepository, jpaRepository);
    when(provider.getAccount()).thenReturn(Account.builder().id(JOE_DOE_ACCOUNT_ID).build());
    when(swanRepository.getByIdAccount(JOE_DOE_ACCOUNT_ID))
        .thenReturn(List.of(swanTransaction1(), swanTransaction2(), swanTransaction3()));
    when(jpaRepository.save(any(HTransaction.class)))
        .thenAnswer(i -> {
          HTransaction argument = i.getArgument(0);
          argument.setId(String.valueOf(randomUUID()));
          argument.setIdAccount(JOE_DOE_ACCOUNT_ID);
          return argument;
        });
    when(jpaRepository.findByIdSwan(any(String.class)))
        .thenAnswer(i -> {
          if (Objects.equals(i.getArgument(0), swanTransaction1().getNode().getId())) {
            return Optional.empty();
          }
          return Optional.of(HTransaction.builder()
              .id(String.valueOf(randomUUID()))
              .idSwan(i.getArgument(0))
              .idAccount(JOE_DOE_ACCOUNT_ID)
              .build());
        });
    when(categoryRepository.findByIdTransaction(any(String.class))).thenAnswer(i -> {
      String idTransaction = i.getArgument(0);
      if (Objects.equals(idTransaction, swanTransaction1().getNode().getId())) {
        return TransactionCategory.builder()
            .type(swanTransaction1().getNode().getLabel())
            .build();
      } else if (Objects.equals(idTransaction, swanTransaction2().getNode().getId())) {
        return TransactionCategory.builder()
            .type(swanTransaction2().getNode().getLabel())
            .build();
      } else if (Objects.equals(idTransaction, swanTransaction3().getNode().getId())) {
        return TransactionCategory.builder()
            .type(swanTransaction3().getNode().getLabel())
            .build();
      }
      return null;
    });
  }

  @Test
  void read_filtered_by_status_ok() {
    List<Transaction> booked =
        transactionRepositoryImpl.findByAccountIdAndStatus(JOE_DOE_ACCOUNT_ID,
            TransactionStatus.BOOKED);
    List<Transaction> bookedBetweenInstants =
        transactionRepositoryImpl.findByAccountIdAndStatusBetweenInstants(JOE_DOE_ACCOUNT_ID,
            TransactionStatus.BOOKED, swanTransaction3().getNode().getCreatedAt(),
            swanTransaction2().getNode().getCreatedAt());

    assertTrue(booked.stream()
        .allMatch(transaction -> transaction.getStatus().equals(TransactionStatus.BOOKED)));
    assertTrue(bookedBetweenInstants.stream().allMatch(transaction ->
        transaction.getPaymentDatetime().isAfter(swanTransaction3().getNode().getCreatedAt())
            && transaction.getPaymentDatetime()
            .isBefore(swanTransaction2().getNode().getCreatedAt())));
  }
}
