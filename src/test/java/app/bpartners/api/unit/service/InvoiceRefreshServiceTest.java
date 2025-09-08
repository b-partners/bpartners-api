package app.bpartners.api.unit.service;

import static app.bpartners.api.endpoint.rest.model.EnableStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.PaymentStatus.UNPAID;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.PaymentRequest;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.PaymentRequestRepository;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.service.invoice.InvoicePDFProcessor;
import app.bpartners.api.service.invoice.InvoiceRefreshService;
import app.bpartners.api.service.invoice.InvoiceService;
import app.bpartners.api.service.user.UserService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InvoiceRefreshServiceTest {
  InvoiceRefreshService subject;
  InvoiceService invoiceServiceMock;
  UserService userServiceMock;
  PaymentRequestRepository paymentRequestRepositoryMock;
  InvoiceJpaRepository invoiceJpaRepositoryMock;
  InvoicePDFProcessor invoicePDFProcessorMock;

  @BeforeEach
  void setUp() {
    invoiceServiceMock = mock(InvoiceService.class);
    userServiceMock = mock(UserService.class);
    paymentRequestRepositoryMock = mock(PaymentRequestRepository.class);
    invoiceJpaRepositoryMock = mock(InvoiceJpaRepository.class);
    invoicePDFProcessorMock = mock(InvoicePDFProcessor.class);
    subject =
        new InvoiceRefreshService(
            invoiceServiceMock,
            userServiceMock,
            paymentRequestRepositoryMock,
            invoiceJpaRepositoryMock,
            invoicePDFProcessorMock);
  }

  @Test
  void refresh_invoice() {
    var user = User.builder().id("userId").status(ENABLED).build();
    when(userServiceMock.findAll()).thenReturn(List.of(user));
    var invoice =
        Invoice.builder()
            .id("invoiceId")
            .updatedAt(now().minus(5, MINUTES))
            .paymentType(app.bpartners.api.endpoint.rest.model.Invoice.PaymentTypeEnum.CASH)
            .ref("BROUILLON-")
            .user(user)
            .build();
    when(invoiceServiceMock.getInvoices(any(), any(), any(), anyList(), any(), any(), anyList()))
        .thenReturn(List.of(invoice));
    var paymentRequest = PaymentRequest.builder().status(UNPAID).build();
    when(paymentRequestRepositoryMock.findAllByReference(any()))
        .thenReturn(List.of(paymentRequest));
    var hInvoice = HInvoice.builder().build();
    when(invoiceJpaRepositoryMock.getById(any())).thenReturn(hInvoice);
    when(invoiceJpaRepositoryMock.save(any())).thenReturn(hInvoice);
    doNothing().when(invoicePDFProcessorMock).accept(any());

    subject.refreshInvoices();

    verify(userServiceMock, times(1)).findAll();
    verify(invoiceServiceMock, times(1))
        .getInvoices(any(), any(), any(), anyList(), any(), any(), anyList());
    verify(paymentRequestRepositoryMock, times(1)).findAllByReference(any());
    verify(invoiceJpaRepositoryMock, times(1)).getById(any());
    verify(invoiceJpaRepositoryMock, times(1)).save(any());
    verify(invoicePDFProcessorMock, times(1)).accept(any());
  }
}
