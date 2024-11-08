package app.bpartners.api.unit.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.User;
import app.bpartners.api.model.mapper.InvoiceMapper;
import app.bpartners.api.model.mapper.InvoiceProductMapper;
import app.bpartners.api.model.mapper.PaymentRequestMapper;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.implementation.InvoiceRepositoryImpl;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InvoiceRepositoryImplTest {
  InvoiceJpaRepository jpaRepository = mock(InvoiceJpaRepository.class);
  InvoiceMapper mapper = mock(InvoiceMapper.class);
  InvoiceProductMapper productMapper = mock(InvoiceProductMapper.class);
  EntityManager entityManager = mock(EntityManager.class);
  UserRepository userRepository = mock(UserRepository.class);
  PaymentRequestMapper paymentRequestMapper = mock(PaymentRequestMapper.class);
  InvoiceRepositoryImpl subject =
      new InvoiceRepositoryImpl(
          jpaRepository,
          mapper,
          productMapper,
          entityManager,
          userRepository,
          paymentRequestMapper);

  @Test
  void find_by_id() {
    var hInvoice = HInvoice.builder().idUser("").build();
    var user = new User();
    var invoice = Invoice.builder().build();
    when(jpaRepository.findById(any())).thenReturn(Optional.of(hInvoice));
    when(userRepository.getById(any())).thenReturn(user);
    when(mapper.toDomain(any(), any())).thenReturn(invoice);

    var actual = subject.findById("");

    assertEquals(invoice, actual);
  }
}
