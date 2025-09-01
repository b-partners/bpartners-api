package app.bpartners.api.unit.mapper;

import static app.bpartners.api.endpoint.rest.model.EnableStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.PaymentMethod.CASH;
import static app.bpartners.api.endpoint.rest.model.PaymentStatus.UNPAID;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.PaymentHistoryStatus;
import app.bpartners.api.model.PaymentRequest;
import app.bpartners.api.model.mapper.DashboardConf;
import app.bpartners.api.model.mapper.PaymentRequestMapper;
import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PaymentRequestMapperTest {
  DashboardConf dashboardConfMock = mock();
  PaymentRequestMapper subject = new PaymentRequestMapper(dashboardConfMock);

  @Test
  void to_entity() {
    var paymentHistoryStatus =
        PaymentHistoryStatus.builder()
            .paymentMethod(CASH)
            .updatedAt(Instant.now().minus(1, DAYS))
            .userUpdated(true)
            .build();
    var domain =
        PaymentRequest.builder()
            .id("id")
            .invoiceId("invoiceId")
            .idUser("idUser")
            .externalId("externalId")
            .paymentUrl("paymentUrl")
            .label("label")
            .comment("comment")
            .payerEmail("payerEmail")
            .reference("reference")
            .paymentDueDate(LocalDate.now().plusDays(10))
            .amount(
                Fraction.builder()
                    .denominator(BigInteger.valueOf(5))
                    .numerator(BigInteger.valueOf(20))
                    .build())
            .paymentHistoryStatus(paymentHistoryStatus)
            .status(UNPAID)
            .build();
    var existing =
        HPaymentRequest.builder().createdDatetime(Instant.now().minus(10, MINUTES)).build();

    var actual = subject.toEntity(domain, existing);

    var expected =
        HPaymentRequest.builder()
            .id(domain.getId())
            .idInvoice(domain.getInvoiceId())
            .idUser(domain.getIdUser())
            .sessionId(domain.getExternalId())
            .paymentUrl(domain.getPaymentUrl())
            .label(domain.getLabel())
            .comment(domain.getComment())
            .payerEmail(domain.getPayerEmail())
            .payerName(domain.getPayerName())
            .reference(domain.getReference())
            .paymentDueDate(domain.getPaymentDueDate())
            .amount(domain.getAmount().toString())
            .createdDatetime(existing.getCreatedDatetime())
            .status(domain.getStatus())
            .enableStatus(ENABLED)
            .paymentMethod(paymentHistoryStatus.getPaymentMethod())
            .paymentStatusUpdatedAt(paymentHistoryStatus.getUpdatedAt())
            .userUpdated(paymentHistoryStatus.getUserUpdated())
            .paymentStatusUpdatedAt(paymentHistoryStatus.getUpdatedAt())
            .build();
    assertEquals(expected, actual);
  }

  @Test
  void to_domain() {
    var entity =
        HPaymentRequest.builder()
            .id("id")
            .idInvoice("invoiceId")
            .idUser("idUser")
            .sessionId("sessionId")
            .paymentUrl("paymentUrl")
            .label("label")
            .comment("comment")
            .payerEmail("payerEmail")
            .payerName("payerName")
            .reference("reference")
            .paymentDueDate(LocalDate.now().plusDays(10))
            .amount("20/5")
            .status(UNPAID)
            .enableStatus(ENABLED)
            .paymentMethod(CASH)
            .userUpdated(true)
            .paymentStatusUpdatedAt(Instant.now().minus(10, MINUTES))
            .build();

    var actual = subject.toDomain(entity);

    var expected =
        PaymentRequest.builder()
            .id(entity.getId())
            .invoiceId(entity.getIdInvoice())
            .idUser(entity.getIdUser())
            .externalId(entity.getSessionId())
            .paymentUrl(entity.getPaymentUrl())
            .label(entity.getLabel())
            .comment(entity.getComment())
            .payerEmail(entity.getPayerEmail())
            .payerName(entity.getPayerName())
            .reference(entity.getReference())
            .paymentDueDate(entity.getPaymentDueDate())
            .amount(parseFraction(entity.getAmount()))
            .createdDatetime(actual.getCreatedDatetime())
            .status(entity.getStatus())
            .enableStatus(entity.getEnableStatus())
            .paymentHistoryStatus(actual.getPaymentHistoryStatus())
            .build();

    assertEquals(expected, actual);
  }
}
