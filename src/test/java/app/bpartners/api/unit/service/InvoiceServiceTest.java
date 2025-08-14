package app.bpartners.api.unit.service;

import static app.bpartners.api.endpoint.rest.model.EnableStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.DRAFT;
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
import app.bpartners.api.service.invoice.CustomerInvoiceValidator;
import app.bpartners.api.service.invoice.InvoicePDFProcessor;
import app.bpartners.api.service.invoice.InvoiceService;
import app.bpartners.api.service.invoice.InvoiceValidator;
import app.bpartners.api.service.payment.CreatePaymentRegulationComputing;
import app.bpartners.api.service.payment.PaymentInitiationService;
import app.bpartners.api.service.payment.PaymentService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InvoiceServiceTest {
  InvoiceRepository repositoryMock = mock(InvoiceRepository.class);
  PaymentInitiationService pisMock = mock(PaymentInitiationService.class);
  PaymentRequestRepository paymentRepositoryMock = mock(PaymentRequestRepository.class);
  InvoicePDFProcessor invoicePDFProcessorMock = mock(InvoicePDFProcessor.class);
  CreatePaymentRegulationComputing paymentRegulationComputingMock =
      mock(CreatePaymentRegulationComputing.class);
  PaymentService paymentServiceMock = mock(PaymentService.class);
  InvoiceValidator invoiceValidatorMock = mock(InvoiceValidator.class);
  CustomerInvoiceValidator customerInvoiceValidatorMock = mock(CustomerInvoiceValidator.class);
  UserRepository userRepositoryMock = mock(UserRepository.class);
  EventProducer eventProducerMock = mock(EventProducer.class);

  InvoiceService subject =
      new InvoiceService(
          repositoryMock,
          pisMock,
          paymentRepositoryMock,
          invoicePDFProcessorMock,
          paymentRegulationComputingMock,
          paymentServiceMock,
          invoiceValidatorMock,
          customerInvoiceValidatorMock,
          eventProducerMock,
          userRepositoryMock);

  User user() {
    return User.builder().id(null).accountHolders(List.of(AccountHolder.builder().build())).build();
  }

  @Test
  void crupdate_subscription_invoice() {
    var accountHolder = AccountHolder.builder().subjectToVat(false).build();
    var user =
        User.builder()
            .preferredAccountId("preferredAccountId")
            .accountHolders(List.of(accountHolder))
            .build();
    var product = InvoiceProduct.builder().build();
    var paymentRequest = PaymentRequest.builder().enableStatus(ENABLED).build();
    var paymentRegulation =
        CreatePaymentRegulation.builder().paymentRequest(paymentRequest).build();
    var invoice =
        Invoice.builder()
            .products(List.of(product))
            .fileId("fileId")
            .status(DRAFT)
            .paymentRegulations(List.of(paymentRegulation))
            .user(user)
            .build();
    doNothing().when(invoiceValidatorMock).checkReferenceAvailability(any());
    doNothing().when(customerInvoiceValidatorMock).accept(any());
    when(paymentRegulationComputingMock.computeWithoutPisURL(any()))
        .thenReturn(List.of(paymentRegulation));
    when(repositoryMock.pwFindOptionalById(any())).thenReturn(Optional.ofNullable(invoice));
    when(repositoryMock.save(any())).thenReturn(invoice);
    doNothing().when(invoicePDFProcessorMock).accept(any());

    subject.crupdateSubscriptionInvoice(invoice);

    verify(invoiceValidatorMock).checkReferenceAvailability(any());
    verify(customerInvoiceValidatorMock).accept(any());
    verify(paymentRegulationComputingMock).computeWithoutPisURL(any());
    verify(repositoryMock).pwFindOptionalById(any());
    verify(repositoryMock).save(any());
    verify(invoicePDFProcessorMock).accept(any());
  }

  @Test
  void generate_invoice_export_link() {
    List<InvoiceStatus> providedStatues = List.of();
    var providedArchiveStatus = ArchiveStatus.ENABLED;
    var from = LocalDate.now();
    var to = LocalDate.now().plusDays(1);
    when(userRepositoryMock.getByIdAccount(ACCOUNT_ID)).thenReturn(user());
    var invoice1 = Invoice.builder().build();
    var invoice2 = Invoice.builder().build();
    var invoices = List.of(invoice1, invoice2);
    when(repositoryMock.findAllByIdUserAndCriteria(any(), any(), any(), any(), any(), any()))
        .thenReturn(invoices);

    var actual =
        subject.generateInvoicesExportLink(
            ACCOUNT_ID, providedStatues, providedArchiveStatus, from, to);

    var expected =
        PreSignedLink.builder()
            .value("Aucune facture liée au compte id=other_joe_account_id")
            .expirationDelay(null)
            .updatedAt(actual.getUpdatedAt())
            .build();
    assertEquals(expected, actual);
    verify(eventProducerMock, never()).accept(any());
  }

  @Test
  void update_payment_status() {
    var savedPaymentRequest =
        PaymentRequest.builder()
            .id("PAYMENT_REQUEST_ID")
            .invoiceId(null)
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
    when(paymentServiceMock.filterByPaymentId(any(), any(), any())).thenReturn(paymentRequest);
    when(paymentRepositoryMock.save(any())).thenReturn(savedPaymentRequest);
    when(repositoryMock.getById(any())).thenReturn(invoice);
    doNothing().when(invoicePDFProcessorMock).accept(any());

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
            .fileId(null)
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
