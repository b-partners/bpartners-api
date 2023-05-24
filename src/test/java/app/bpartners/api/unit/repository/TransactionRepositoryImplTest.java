package app.bpartners.api.unit.repository;

import app.bpartners.api.model.UserToken;
import app.bpartners.api.model.mapper.InvoiceMapper;
import app.bpartners.api.model.mapper.TransactionMapper;
import app.bpartners.api.repository.TransactionCategoryRepository;
import app.bpartners.api.repository.connectors.transaction.TransactionConnectorRepository;
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
  UserService userServiceMock;
  InvoiceJpaRepository invoiceJpaRepositoryMock;
  TransactionConnectorRepository transactionConnectorRepositoryMock;


  @BeforeEach
  void setUp() {
    invoiceMapperMock = mock(InvoiceMapper.class);
    transactionMapperMock = new TransactionMapper(invoiceMapperMock);
    categoryRepositoryMock = mock(TransactionCategoryRepository.class);
    transactionJpaRepositoryMock = mock(TransactionJpaRepository.class);
    invoiceJpaRepositoryMock = mock(InvoiceJpaRepository.class);
    transactionConnectorRepositoryMock = mock(TransactionConnectorRepository.class);
    subject = new TransactionRepositoryImpl(transactionMapperMock, categoryRepositoryMock,
        transactionJpaRepositoryMock, invoiceJpaRepositoryMock, transactionConnectorRepositoryMock);

    when(userServiceMock.getLatestTokenByAccount(any())).thenReturn(
        UserToken.builder().build());
  }

  //TODO: add test
  //  @Test
  //  void read_filtered_by_status_ok() {
  //
  //  }
}
