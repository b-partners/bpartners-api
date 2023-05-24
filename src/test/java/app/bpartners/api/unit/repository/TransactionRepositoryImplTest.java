package app.bpartners.api.unit.repository;

import app.bpartners.api.model.UserToken;
import app.bpartners.api.model.mapper.InvoiceMapper;
import app.bpartners.api.model.mapper.TransactionMapper;
import app.bpartners.api.repository.TransactionCategoryRepository;
import app.bpartners.api.repository.bridge.repository.BridgeTransactionRepository;
import app.bpartners.api.repository.implementation.TransactionRepositoryImpl;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.TransactionJpaRepository;
import app.bpartners.api.service.UserService;
import org.junit.jupiter.api.BeforeEach;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TransactionRepositoryImplTest {
  TransactionRepositoryImpl subject;
  TransactionMapper transactionMapperMock;
  TransactionCategoryRepository categoryRepositoryMock;
  TransactionJpaRepository transactionJpaRepositoryMock;
  InvoiceMapper invoiceMapperMock;
  BridgeTransactionRepository bridgeTransactionRepositoryMock;
  UserService userServiceMock;
  InvoiceJpaRepository invoiceJpaRepositoryMock;


  @BeforeEach
  void setUp() {
    invoiceMapperMock = mock(InvoiceMapper.class);
    transactionMapperMock = new TransactionMapper(invoiceMapperMock);
    categoryRepositoryMock = mock(TransactionCategoryRepository.class);
    transactionJpaRepositoryMock = mock(TransactionJpaRepository.class);
    bridgeTransactionRepositoryMock = mock(BridgeTransactionRepository.class);
    userServiceMock = mock(UserService.class);
    invoiceJpaRepositoryMock = mock(InvoiceJpaRepository.class);
    subject = new TransactionRepositoryImpl(transactionMapperMock, categoryRepositoryMock,
        transactionJpaRepositoryMock, bridgeTransactionRepositoryMock, userServiceMock,
        invoiceJpaRepositoryMock);

    when(userServiceMock.getLatestTokenByAccount(any())).thenReturn(
        UserToken.builder().build());
  }

  //TODO: add test
  //  @Test
  //  void read_filtered_by_status_ok() {
  //
  //  }
}
