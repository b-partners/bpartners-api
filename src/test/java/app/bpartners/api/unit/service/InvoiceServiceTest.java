package app.bpartners.api.unit.service;

import static app.bpartners.api.integration.UserTokenServiceIT.ACCOUNT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.rest.model.ArchiveStatus;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.PreSignedLink;
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
    return User.builder().id("").build();
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
}
