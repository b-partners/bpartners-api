package app.bpartners.api.unit.service;

import static app.bpartners.api.endpoint.rest.model.EnableStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.PaymentMethod.CASH;
import static app.bpartners.api.endpoint.rest.model.PaymentStatus.UNPAID;
import static app.bpartners.api.integration.UserTokenServiceIT.ACCOUNT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.rest.model.*;
import app.bpartners.api.model.*;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.CreatePaymentRegulation;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.PaymentRequest;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.PaymentRequestRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.InvoiceService;
import app.bpartners.api.service.PaymentInitiationService;
import app.bpartners.api.service.invoice.CustomerInvoiceValidator;
import app.bpartners.api.service.invoice.InvoicePDFProcessor;
import app.bpartners.api.service.invoice.InvoiceValidator;
import app.bpartners.api.service.payment.CreatePaymentRegulationComputing;
import app.bpartners.api.service.payment.PaymentService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class InvoiceServiceTest {
  InvoiceRepository repositoryMock = mock(InvoiceRepository.class);
  PaymentInitiationService pis = mock(PaymentInitiationService.class);
  PaymentRequestRepository paymentRepository = mock(PaymentRequestRepository.class);
  InvoicePDFProcessor invoicePDFProcessor = mock(InvoicePDFProcessor.class);
  CreatePaymentRegulationComputing paymentRegulationComputing =
      mock(CreatePaymentRegulationComputing.class);
  PaymentService paymentService = mock(PaymentService.class);
  InvoiceValidator invoiceValidator = mock(InvoiceValidator.class);
  CustomerInvoiceValidator customerInvoiceValidator = mock(CustomerInvoiceValidator.class);
  EventProducer eventProducer = mock(EventProducer.class);
  UserRepository userRepositoryMock = mock(UserRepository.class);

  InvoiceService subject =
      new InvoiceService(
          repositoryMock,
          pis,
          paymentRepository,
          invoicePDFProcessor,
          paymentRegulationComputing,
          paymentService,
          invoiceValidator,
          customerInvoiceValidator,
          eventProducer,
          userRepositoryMock);

  User user() {
    return User.builder().id("").accountHolders(List.of(AccountHolder.builder().build())).build();
  }

  @Test
  void generate_invoice_export_link() {
    List<InvoiceStatus> providedStatues = List.of();
    var providedArchiveStatus = ArchiveStatus.ENABLED;
    var from = LocalDate.now();
    var to = LocalDate.now().plusDays(1);
    when(userRepositoryMock.getByIdAccount(ACCOUNT_ID)).thenReturn(user());
    var invoices = List.of(Invoice.builder().build());
    when(repositoryMock.findAllByIdUserAndCriteria(any(), any(), any(), any(), any(), any()))
        .thenReturn(invoices);

    var actual =
        subject.generateInvoicesExportLink(
            ACCOUNT_ID, providedStatues, providedArchiveStatus, from, to);

    var expected =
        PreSignedLink.builder()
            .value(null)
            .expirationDelay(null)
            .updatedAt(actual.getUpdatedAt())
            .build();
    assertEquals(expected, actual);
  }

  @Test
  void update_payment_status() {
    var savedPaymentRequest =
        PaymentRequest.builder()
            .id("PAYMENT_REQUEST_ID")
            .invoiceId("")
            .status(UNPAID)
            .paymentHistoryStatus(
                PaymentHistoryStatus.builder()
                    .status(UNPAID)
                    .paymentMethod(CASH)
                    .updatedAt(Instant.now())
                    .userUpdated(true)
                    .build())
            .build();
    var paymentRegulation =
        CreatePaymentRegulation.builder()
            .paymentRequest(
                PaymentRequest.builder()
                    .id("PAYMENT_REQUEST_ID")
                    .enableStatus(ENABLED)
                    .status(UNPAID)
                    .build())
            .build();
    var invoice =
        Invoice.builder()
            .fileId("")
            .products(List.of())
            .user(user())
            .paymentRegulations(List.of(paymentRegulation))
            .build();
    var paymentRequest = PaymentRequest.builder().enableStatus(ENABLED).build();
    when(paymentService.filterByPaymentId(any(), any(), any())).thenReturn(paymentRequest);
    when(paymentRepository.save(any())).thenReturn(savedPaymentRequest);
    when(repositoryMock.getById(any())).thenReturn(invoice);
    doNothing().when(invoicePDFProcessor).accept(any());

    var actual = subject.updatePaymentStatus("invoiceId", "paymentId", CASH);

    assertEquals(invoice, actual);
  }

  @Test
  void duplicate_as_draft() {
    var paymentRequest = PaymentRequest.builder().enableStatus(ENABLED).build();
    var paymentRegulation =
        CreatePaymentRegulation.builder().paymentRequest(paymentRequest).build();
    var invoice =
        Invoice.builder()
            .fileId("")
            .products(List.of())
            .user(user())
            .paymentRegulations(List.of(paymentRegulation))
            .build();
    when(repositoryMock.getById(any())).thenReturn(invoice);
    when(repositoryMock.save(any())).thenReturn(invoice);

    var actual = subject.duplicateAsDraft("invoiceId", "ref");

    assertEquals(invoice, actual);
  }
}
