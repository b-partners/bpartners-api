package app.bpartners.api.unit.mapper;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceDiscount;
import app.bpartners.api.model.User;
import app.bpartners.api.model.mapper.CustomerMapper;
import app.bpartners.api.model.mapper.InvoiceMapper;
import app.bpartners.api.model.mapper.InvoiceProductMapper;
import app.bpartners.api.model.mapper.PaymentRequestMapper;
import app.bpartners.api.repository.jpa.model.HInvoice;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class InvoiceMapperTest {
  CustomerMapper customerMapper = mock();
  InvoiceProductMapper productMapper = mock();
  PaymentRequestMapper paymentRequestMapper = mock();
  InvoiceMapper subject =
      new InvoiceMapper(new ObjectMapper(), customerMapper, productMapper, paymentRequestMapper);

  @Test
  void to_domain_reads_a_persisted_credit_note_as_a_credit_note() {
    assertTrue(subject.toDomain(entity(true)).isCreditNote());
  }

  @Test
  void to_domain_reads_a_persisted_regular_invoice_as_not_a_credit_note() {
    assertFalse(subject.toDomain(entity(false)).isCreditNote());
  }

  @Test
  void to_domain_reads_a_null_credit_note_column_as_not_a_credit_note() {
    assertFalse(subject.toDomain(entity(null)).isCreditNote());
  }

  @Test
  void to_domain_with_user_keeps_the_credit_note_flag() {
    var user = User.builder().id("user_id").build();

    var actual = subject.toDomain(entity(true), user);

    assertTrue(actual.isCreditNote());
    assertEquals(user, actual.getUser());
  }

  @Test
  void to_entity_persists_the_credit_note_flag() {
    assertEquals(
        Boolean.TRUE, subject.toEntity(domain(true), new ArrayList<>(), List.of()).getCreditNote());
    assertEquals(
        Boolean.FALSE,
        subject.toEntity(domain(false), new ArrayList<>(), List.of()).getCreditNote());
  }

  @Test
  void a_regular_invoice_is_not_a_credit_note_by_default() {
    assertFalse(Invoice.builder().build().isCreditNote());
  }

  private static HInvoice entity(Boolean creditNote) {
    return HInvoice.builder()
        .id("invoice_id")
        .ref("REF-1")
        .status(CONFIRMED)
        .creditNote(creditNote)
        .products(List.of())
        .paymentRequests(List.of())
        .build();
  }

  private static Invoice domain(boolean creditNote) {
    return Invoice.builder()
        .id("invoice_id")
        .ref("REF-1")
        .status(CONFIRMED)
        .creditNote(creditNote)
        .user(User.builder().id("user_id").build())
        .delayPenaltyPercent(new Fraction())
        .discount(InvoiceDiscount.builder().percentValue(new Fraction()).build())
        .build();
  }
}
