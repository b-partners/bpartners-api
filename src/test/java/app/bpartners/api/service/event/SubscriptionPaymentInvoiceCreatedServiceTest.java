package app.bpartners.api.service.event;

import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE;
import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static app.bpartners.api.model.subscription.BillingInterval.YEARLY;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.EmailRecipientsUpdateRequested;
import app.bpartners.api.endpoint.event.model.SubscriptionPaymentInvoiceCreated;
import app.bpartners.api.endpoint.rest.model.EmailRecipientType;
import app.bpartners.api.file.FileWriter;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.Attachment;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceProduct;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.subscription.SubscriptionPayment;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.SubscriptionPaymentRepository;
import app.bpartners.api.service.EmailInvoiceResolver;
import app.bpartners.api.service.accountholder.EmailRecipientService;
import app.bpartners.api.service.aws.S3Service;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import javax.mail.MessagingException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SubscriptionPaymentInvoiceCreatedServiceTest {
  private static final byte[] PDF_BYTES = new byte[] {1, 2, 3};

  InvoiceRepository invoiceRepository = mock();
  SubscriptionPaymentRepository subscriptionPaymentRepository = mock();
  S3Service s3Service = mock();
  FileWriter fileWriter = mock();
  SesService mailer = mock();
  EmailRecipientService emailRecipientService = mock();
  UserRepository userRepository = mock();
  EventProducer<EmailRecipientsUpdateRequested> eventProducer = mock();
  EmailInvoiceResolver emailInvoiceResolver =
      new EmailInvoiceResolver(emailRecipientService, userRepository, eventProducer);
  SubscriptionPaymentInvoiceCreatedService subject =
      new SubscriptionPaymentInvoiceCreatedService(
          invoiceRepository,
          subscriptionPaymentRepository,
          s3Service,
          fileWriter,
          mailer,
          new TemplateResolverEngine(),
          new CustomDateFormatter(),
          emailInvoiceResolver);

  SubscriptionPaymentInvoiceCreatedServiceTest() {
    when(s3Service.downloadFile(any(), anyString(), anyString()))
        .thenReturn(new File("invoice.pdf"));
    when(fileWriter.writeAsByte(any(File.class))).thenReturn(PDF_BYTES);
  }

  @Test
  void sends_the_invoice_to_the_subscriber_with_tech_in_copy() throws Exception {
    givenInvoiceAndPayment(someInvoice(), somePayment());

    subject.accept(someEvent());

    var recipientCaptor = ArgumentCaptor.forClass(String.class);
    var copyCaptor = ArgumentCaptor.forClass(String.class);
    var subjectCaptor = ArgumentCaptor.forClass(String.class);
    verify(mailer)
        .sendEmail(
            recipientCaptor.capture(),
            copyCaptor.capture(),
            subjectCaptor.capture(),
            anyString(),
            anyList());
    assertEquals("subscriber@email.com", recipientCaptor.getValue());
    assertEquals("tech@birdia.fr", copyCaptor.getValue());
    assertEquals(
        "[BIRDIA] Votre facture d'abonnement REF-04032026103000 est disponible",
        subjectCaptor.getValue());
  }

  @Test
  void sends_to_the_single_configured_invoice_recipient_when_present() throws Exception {
    givenInvoiceAndPayment(someInvoice(), somePayment());
    givenRecipientUserOfCustomerEmail();
    when(emailRecipientService.getEmails("account_holder_id", EmailRecipientType.INVOICE))
        .thenReturn(List.of("compta@client.fr"));

    subject.accept(someEvent());

    var recipientCaptor = ArgumentCaptor.forClass(String.class);
    verify(mailer)
        .sendEmail(recipientCaptor.capture(), anyString(), anyString(), anyString(), anyList());
    assertEquals("compta@client.fr", recipientCaptor.getValue());
    verify(eventProducer, never()).accept(anyList());
  }

  @Test
  void keeps_only_the_first_configured_invoice_recipient_when_several() throws Exception {
    givenInvoiceAndPayment(someInvoice(), somePayment());
    givenRecipientUserOfCustomerEmail();
    when(emailRecipientService.getEmails("account_holder_id", EmailRecipientType.INVOICE))
        .thenReturn(List.of("compta@client.fr", "admin@client.fr"));

    subject.accept(someEvent());

    var recipientCaptor = ArgumentCaptor.forClass(String.class);
    verify(mailer)
        .sendEmail(recipientCaptor.capture(), anyString(), anyString(), anyString(), anyList());
    assertEquals("compta@client.fr", recipientCaptor.getValue());
    verify(eventProducer, never()).accept(anyList());
  }

  @Test
  void requests_async_recipients_update_and_falls_back_when_none_configured() throws Exception {
    givenInvoiceAndPayment(someInvoice(), somePayment());
    givenRecipientUserOfCustomerEmail();
    when(emailRecipientService.getEmails("account_holder_id", EmailRecipientType.INVOICE))
        .thenReturn(List.of());

    subject.accept(someEvent());

    var recipientCaptor = ArgumentCaptor.forClass(String.class);
    verify(mailer)
        .sendEmail(recipientCaptor.capture(), anyString(), anyString(), anyString(), anyList());
    assertEquals("subscriber@email.com", recipientCaptor.getValue());
    var eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(eventsCaptor.capture());
    var requested = (EmailRecipientsUpdateRequested) eventsCaptor.getValue().getFirst();
    assertEquals("account_holder_id", requested.getAccountHolderId());
    assertEquals("recipient_user_id", requested.getUserId());
    assertEquals(EmailRecipientType.INVOICE, requested.getType());
  }

  @Test
  void attaches_the_invoice_pdf_of_the_issuing_admin() throws Exception {
    givenInvoiceAndPayment(someInvoice(), somePayment());

    subject.accept(someEvent());

    verify(s3Service).downloadFile(INVOICE, "file_id", "admin_user_id");
    var attachmentsCaptor = ArgumentCaptor.forClass(List.class);
    verify(mailer)
        .sendEmail(anyString(), anyString(), anyString(), anyString(), attachmentsCaptor.capture());
    var attachment = (Attachment) attachmentsCaptor.getValue().getFirst();
    assertEquals("REF-04032026103000", attachment.getName());
    assertEquals(PDF_BYTES, attachment.getContent());
  }

  @Test
  void renders_the_subscription_summary_in_the_html_body() {
    givenInvoiceAndPayment(someInvoice(), somePayment());

    subject.accept(someEvent());

    var body = capturedHtmlBody();
    assertTrue(body.contains("Buyer SARL"));
    assertTrue(body.contains("REF-04032026103000"));
    assertTrue(body.contains("Essentiel"));
    assertTrue(body.contains("Facturation"));
    assertTrue(body.contains("Mensuelle"));
    assertTrue(body.contains("04/03/2026 au 04/04/2026"));
    assertTrue(body.contains("40,83 €"));
    assertTrue(body.contains("49,00 €"));
  }

  @Test
  void renders_the_yearly_billing_interval_when_the_subscription_is_annual() {
    givenInvoiceAndPayment(
        someInvoice(), somePayment().toBuilder().billingInterval(YEARLY).build());

    subject.accept(someEvent());

    assertTrue(capturedHtmlBody().contains("Annuelle"));
  }

  @Test
  void hides_the_billing_interval_row_when_it_is_unknown() {
    givenInvoiceAndPayment(someInvoice(), somePayment().toBuilder().billingInterval(null).build());

    subject.accept(someEvent());

    assertFalse(capturedHtmlBody().contains("Facturation"));
  }

  @Test
  void renders_a_modern_html_body() {
    givenInvoiceAndPayment(someInvoice(), somePayment());

    subject.accept(someEvent());

    var body = capturedHtmlBody();
    assertTrue(body.contains("<style>"));
    assertTrue(body.contains("max-width: 560px"));
    assertTrue(body.contains("@media (max-width: 600px)"));
    assertTrue(body.contains("L'équipe BIRDIA"));
    assertFalse(body.contains("${"));
    assertFalse(body.contains("th:text"));
  }

  @Test
  void falls_back_on_the_payment_date_when_the_billed_period_is_unknown() {
    givenInvoiceAndPayment(
        someInvoice(),
        somePayment().toBuilder().periodStartDatetime(null).periodEndDatetime(null).build());

    subject.accept(someEvent());

    assertTrue(capturedHtmlBody().contains("04/03/2026"));
  }

  @Test
  void falls_back_on_the_invoice_line_when_the_payment_is_gone() {
    when(invoiceRepository.findById("invoice_id")).thenReturn(someInvoice());
    when(subscriptionPaymentRepository.findById("subscription_payment_id"))
        .thenReturn(Optional.empty());

    subject.accept(someEvent());

    assertTrue(capturedHtmlBody().contains("Abonnement Essentiel du 04/03/2026 au 04/04/2026"));
  }

  @Test
  void falls_back_on_a_generic_label_when_neither_payment_nor_invoice_line_is_available() {
    when(invoiceRepository.findById("invoice_id"))
        .thenReturn(someInvoice().toBuilder().products(List.of()).build());
    when(subscriptionPaymentRepository.findById("subscription_payment_id"))
        .thenReturn(Optional.empty());

    subject.accept(someEvent());

    assertTrue(capturedHtmlBody().contains("Abonnement"));
  }

  @Test
  void sends_nothing_when_the_invoice_is_gone() throws Exception {
    when(invoiceRepository.findById("invoice_id")).thenReturn(null);

    subject.accept(someEvent());

    verify(mailer, never()).sendEmail(any(), any(), any(), any(), anyList());
    verify(s3Service, never()).downloadFile(any(), any(), any());
  }

  @Test
  void sends_nothing_when_the_customer_has_no_email() throws Exception {
    when(invoiceRepository.findById("invoice_id"))
        .thenReturn(
            someInvoice().toBuilder()
                .customer(Customer.builder().id("customer_id").name("Buyer SARL").build())
                .build());

    subject.accept(someEvent());

    verify(mailer, never()).sendEmail(any(), any(), any(), any(), anyList());
    verify(s3Service, never()).downloadFile(any(), any(), any());
  }

  @Test
  void sends_nothing_when_the_invoice_has_no_customer() throws Exception {
    when(invoiceRepository.findById("invoice_id"))
        .thenReturn(someInvoice().toBuilder().customer(null).build());

    subject.accept(someEvent());

    verify(mailer, never()).sendEmail(any(), any(), any(), any(), anyList());
  }

  @Test
  void renders_without_throwing_when_the_invoice_carries_no_date() {
    givenInvoiceAndPayment(
        someInvoice().toBuilder().sendingDate(null).createdAt(null).build(), somePayment());

    assertDoesNotThrow(() -> subject.accept(someEvent()));
  }

  @Test
  void a_failing_mail_is_reported_as_a_server_error() throws Exception {
    givenInvoiceAndPayment(someInvoice(), somePayment());
    doThrow(new MessagingException("ses down"))
        .when(mailer)
        .sendEmail(anyString(), anyString(), anyString(), anyString(), anyList());

    assertThrows(ApiException.class, () -> subject.accept(someEvent()));
  }

  @Test
  void retries_the_mail_for_at_most_five_minutes() {
    var event = someEvent();

    assertEquals(Duration.ofMinutes(5L), event.maxConsumerDuration());
    assertEquals(Duration.ofMinutes(1L), event.maxConsumerBackoffBetweenRetries());
  }

  @SneakyThrows
  private String capturedHtmlBody() {
    var bodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(mailer)
        .sendEmail(anyString(), anyString(), anyString(), bodyCaptor.capture(), anyList());
    return bodyCaptor.getValue();
  }

  private void givenInvoiceAndPayment(Invoice invoice, SubscriptionPayment subscriptionPayment) {
    when(invoiceRepository.findById("invoice_id")).thenReturn(invoice);
    when(subscriptionPaymentRepository.findById("subscription_payment_id"))
        .thenReturn(Optional.of(subscriptionPayment));
  }

  private SubscriptionPaymentInvoiceCreated someEvent() {
    return SubscriptionPaymentInvoiceCreated.builder()
        .invoiceId("invoice_id")
        .subscriptionPaymentId("subscription_payment_id")
        .build();
  }

  private void givenRecipientUserOfCustomerEmail() {
    when(userRepository.findByEmail("subscriber@email.com"))
        .thenReturn(
            Optional.of(
                User.builder()
                    .id("recipient_user_id")
                    .accountHolders(
                        List.of(AccountHolder.builder().id("account_holder_id").build()))
                    .build()));
  }

  private Invoice someInvoice() {
    return Invoice.builder()
        .id("invoice_id")
        .ref("REF-04032026103000")
        .fileId("file_id")
        .user(User.builder().id("admin_user_id").build())
        .customer(
            Customer.builder()
                .id("customer_id")
                .name("Buyer SARL")
                .email("subscriber@email.com")
                .build())
        .sendingDate(LocalDate.of(2026, 3, 4))
        .products(
            List.of(
                InvoiceProduct.builder()
                    .description("Abonnement Essentiel du 04/03/2026 au 04/04/2026")
                    .quantity(1)
                    .build()))
        .totalPriceWithoutVat(parseFraction(4083))
        .totalPriceWithVat(parseFraction(4900))
        .build();
  }

  private SubscriptionPayment somePayment() {
    return SubscriptionPayment.builder()
        .id("subscription_payment_id")
        .label("Abonnement Essentiel du 04/03/2026 au 04/04/2026")
        .subscriptionProduct(SubscriptionProduct.builder().name("Essentiel").build())
        .billingInterval(MONTHLY)
        .amountInCentsWithoutVat(4_083L)
        .amountInCentsWithVat(4_900L)
        .vatPercent(2_000L)
        .periodStartDatetime(Instant.parse("2026-03-04T09:30:00Z"))
        .periodEndDatetime(Instant.parse("2026-04-04T09:30:00Z"))
        .paymentDatetime(Instant.parse("2026-03-04T09:30:00Z"))
        .build();
  }
}
