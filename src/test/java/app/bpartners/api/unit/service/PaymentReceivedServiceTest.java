package app.bpartners.api.unit.service;

import static app.bpartners.api.endpoint.rest.model.Invoice.PaymentTypeEnum.CASH;
import static app.bpartners.api.endpoint.rest.model.PaymentMethod.BANK_TRANSFER;
import static app.bpartners.api.endpoint.rest.model.PaymentStatus.PAID;
import static app.bpartners.api.endpoint.rest.model.PaymentStatus.UNPAID;
import static app.bpartners.api.service.payment.PaymentScheduleService.PAYMENT_CREATED;
import static java.time.LocalDate.now;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.SesConf;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.PaymentRequestJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import app.bpartners.api.service.SnsService;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.invoice.InvoicePDFProcessor;
import app.bpartners.api.service.payment.PaymentReceivedService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentReceivedServiceTest {
  PaymentRequestJpaRepository jpaRepositoryMock = mock();
  SesService sesServiceMock = mock();
  SesConf sesConfMock = mock();
  UserRepository userRepositoryMock = mock();
  InvoiceRepository invoiceRepositoryMock = mock();
  InvoiceJpaRepository invoiceJpaRepositoryMock = mock();
  SnsService snsServiceMock = mock();
  InvoicePDFProcessor invoicePDFProcessorMock = mock();
  TemplateResolverEngine templateResolverEngineMock = mock();
  CustomDateFormatter customDateFormatterMock = mock();
  PaymentReceivedService subject;

  @BeforeEach
  void setUp() {
    subject =
        new PaymentReceivedService(
            jpaRepositoryMock,
            sesServiceMock,
            sesConfMock,
            userRepositoryMock,
            invoiceRepositoryMock,
            invoiceJpaRepositoryMock,
            snsServiceMock,
            invoicePDFProcessorMock,
            templateResolverEngineMock,
            customDateFormatterMock);
  }

  @Test
  void update_payment_statuses() throws MessagingException, IOException {
    var sessionId = "sessionId";
    var paymentStatusMap = Map.of(sessionId, PAYMENT_CREATED);
    var amount = "2/3";
    var savedPaidPayments =
        HPaymentRequest.builder().idInvoice("idInvoice").idUser("userId").amount(amount).build();
    when(jpaRepositoryMock.findBySessionId(anyString()))
        .thenReturn(Optional.ofNullable(savedPaidPayments));
    assert savedPaidPayments != null;
    when(jpaRepositoryMock.saveAll(anyList())).thenReturn(List.of(savedPaidPayments));
    var paymentRequestPaid =
        HPaymentRequest.builder().idUser("userId").status(PAID).amount(amount).build();
    var paymentRequestUnpaid =
        HPaymentRequest.builder()
            .idUser("userId")
            .status(UNPAID)
            .amount(amount)
            .paymentMethod(BANK_TRANSFER)
            .build();
    var invoice =
        HInvoice.builder()
            .id("hInvoiceId")
            .paymentRequests(List.of(paymentRequestPaid, paymentRequestUnpaid))
            .paymentType(CASH)
            .build();
    when(invoiceJpaRepositoryMock.getById(any())).thenReturn(invoice);
    when(invoiceJpaRepositoryMock.save(any())).thenReturn(invoice);
    var retrievedInvoice = app.bpartners.api.model.Invoice.builder().build();
    when(invoiceRepositoryMock.getById(any())).thenReturn(retrievedInvoice);
    doNothing().when(invoicePDFProcessorMock).accept(any());
    var accountHolder = AccountHolder.builder().email("account@holder.com").build();
    var user = User.builder().accountHolders(List.of(accountHolder)).build();
    when(userRepositoryMock.getById(any())).thenReturn(user);
    when(customDateFormatterMock.formatFrenchDate(any(LocalDate.class)))
        .thenReturn(String.valueOf(now()));
    when(templateResolverEngineMock.parseTemplateResolver(any(), any())).thenReturn("emailBody");
    when(sesConfMock.getAdminEmail()).thenReturn("admin@admin.com");
    doNothing().when(sesServiceMock).sendEmail(any(), any(), any(), any(), any());
    doNothing().when(snsServiceMock).pushNotification(anyString(), any());

    subject.updatePaymentStatuses(paymentStatusMap);

    verify(jpaRepositoryMock).findBySessionId(anyString());
    verify(jpaRepositoryMock).saveAll(anyList());
    verify(invoiceJpaRepositoryMock).save(any());
    verify(invoiceRepositoryMock).getById(any());
    verify(invoicePDFProcessorMock).accept(any());
    verify(templateResolverEngineMock).parseTemplateResolver(any(), any());
    verify(sesConfMock).getAdminEmail();
    verify(sesServiceMock)
        .sendEmail(anyString(), any(), anyString(), anyString(), anyList(), anyString());
    verify(snsServiceMock).pushNotification(anyString(), any());
  }
}
