package app.bpartners.api.repository.implementation;

import static app.bpartners.api.endpoint.rest.model.ArchiveStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.User;
import app.bpartners.api.model.mapper.InvoiceMapper;
import app.bpartners.api.model.mapper.InvoiceProductMapper;
import app.bpartners.api.model.mapper.PaymentRequestMapper;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;

class InvoiceRepositoryImplTest {
  InvoiceJpaRepository jpaRepositoryMock = mock();
  InvoiceMapper mapperMock = mock();
  InvoiceProductMapper productMapperMock = mock();
  EntityManager entityManagerMock = mock();
  UserRepository userRepositoryMock = mock();
  PaymentRequestMapper paymentRequestMapperMock = mock();

  InvoiceRepositoryImpl subject =
      new InvoiceRepositoryImpl(
          jpaRepositoryMock,
          mapperMock,
          productMapperMock,
          entityManagerMock,
          userRepositoryMock,
          paymentRequestMapperMock);

  @Test
  void find_sending_years_without_emails_skips_the_query() {
    var actual = subject.findSendingYears("owner", List.of(), "prefix", List.of(PAID), ENABLED);

    assertEquals(List.of(), actual);
    verifyNoInteractions(jpaRepositoryMock);
  }

  @Test
  void find_sending_years_lowercases_emails() {
    var statuses = List.of(CONFIRMED, PAID);
    when(jpaRepositoryMock.findSendingYears(
            "owner", List.of("a@mail.com", "b@mail.com"), "prefix", statuses, ENABLED))
        .thenReturn(List.of(2024, 2025));

    var actual =
        subject.findSendingYears(
            "owner", List.of("A@Mail.com", "b@MAIL.com"), "prefix", statuses, ENABLED);

    assertEquals(List.of(2024, 2025), actual);
  }

  @Test
  void find_all_by_ids_without_ids_skips_the_query() {
    assertEquals(List.of(), subject.findAllByIds(List.of()));

    verifyNoInteractions(jpaRepositoryMock, userRepositoryMock);
  }

  @Test
  void find_all_by_ids_loads_each_owner_once() {
    var first = HInvoice.builder().id("invoice_1").idUser("owner").build();
    var second = HInvoice.builder().id("invoice_2").idUser("owner").build();
    var other = HInvoice.builder().id("invoice_3").idUser("other_owner").build();
    var owner = User.builder().id("owner").build();
    var otherOwner = User.builder().id("other_owner").build();
    var firstDomain = Invoice.builder().id("invoice_1").build();
    var secondDomain = Invoice.builder().id("invoice_2").build();
    var otherDomain = Invoice.builder().id("invoice_3").build();
    when(jpaRepositoryMock.findAllById(List.of("invoice_1", "invoice_2", "invoice_3")))
        .thenReturn(List.of(first, second, other));
    when(userRepositoryMock.getById("owner")).thenReturn(owner);
    when(userRepositoryMock.getById("other_owner")).thenReturn(otherOwner);
    when(mapperMock.toDomain(first, owner)).thenReturn(firstDomain);
    when(mapperMock.toDomain(second, owner)).thenReturn(secondDomain);
    when(mapperMock.toDomain(other, otherOwner)).thenReturn(otherDomain);

    var actual = subject.findAllByIds(List.of("invoice_1", "invoice_2", "invoice_3"));

    assertEquals(List.of(firstDomain, secondDomain, otherDomain), actual);
    verify(userRepositoryMock, times(1)).getById("owner");
    verify(userRepositoryMock, times(1)).getById("other_owner");
    verify(mapperMock, never()).toDomain(any(HInvoice.class));
  }
}
