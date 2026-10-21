package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static app.bpartners.api.model.subscription.BillingInterval.YEARLY;
import static app.bpartners.api.service.subscription.SubscriptionInvoiceMailer.SUBSCRIPTION_INVOICE_MAIL_TEMPLATE;
import static app.bpartners.api.service.subscription.SubscriptionInvoiceMailer.TECH_RECIPIENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.file.FileWriter;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceProduct;
import app.bpartners.api.model.User;
import app.bpartners.api.model.subscription.SubscriptionInvoicePeriod;
import app.bpartners.api.model.subscription.SubscriptionPayment;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.SubscriptionInvoicePeriodRepository;
import app.bpartners.api.service.EmailInvoiceResolver;
import app.bpartners.api.service.accountholder.EmailRecipientService;
import app.bpartners.api.service.aws.S3Service;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.io.File;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SubscriptionInvoiceMailerTest {
  S3Service s3Service = mock();
  FileWriter fileWriter = mock();
  SesService mailer = mock();
  EmailRecipientService emailRecipientService = mock();
  UserRepository userRepository = mock();
  EventProducer eventProducer = mock();
  SubscriptionInvoicePeriodRepository subscriptionInvoicePeriodRepository = mock();

  SubscriptionInvoiceMailer subject =
      new SubscriptionInvoiceMailer(
          s3Service,
          fileWriter,
          mailer,
          new TemplateResolverEngine(),
          new CustomDateFormatter(),
          new EmailInvoiceResolver(emailRecipientService, userRepository, eventProducer),
          subscriptionInvoicePeriodRepository);

  SubscriptionInvoiceMailerTest() {
    when(s3Service.downloadFile(any(), anyString(), anyString()))
        .thenReturn(new File("invoice.pdf"));
    when(fileWriter.writeAsByte(any(File.class))).thenReturn(new byte[] {1, 2, 3});
    when(subscriptionInvoicePeriodRepository.findByInvoiceId(anyString()))
        .thenReturn(Optional.empty());
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
  }

  @Test
  void tech_recipient_is_used_when_the_subscriber_must_not_be_reached() throws Exception {
    subject.send(someInvoice(), somePayment(), SUBSCRIPTION_INVOICE_MAIL_TEMPLATE, TECH_RECIPIENT);

    var recipientCaptor = ArgumentCaptor.forClass(String.class);
    verify(mailer)
        .sendEmail(recipientCaptor.capture(), anyString(), anyString(), anyString(), anyList());
    assertEquals(TECH_RECIPIENT, recipientCaptor.getValue());
  }

  @Test
  void subscriber_recipient_is_the_customer_email_when_nothing_is_configured() {
    assertEquals("s3d@client.fr", subject.subscriberRecipientOf(someInvoice()));
  }

  @Test
  void upgrade_invoice_mail_shows_the_plan_name_and_the_whole_coverage_period() throws Exception {
    var summary =
        new SubscriptionMailSummary(
            "Expert", MONTHLY, LocalDate.of(2026, 10, 14), LocalDate.of(2027, 9, 30));

    assertTrue(
        subject.sendWithSummary(
            upgradeInvoice(), summary, SUBSCRIPTION_INVOICE_MAIL_TEMPLATE, "s3d@client.fr"));

    var body = sentHtmlBody();
    assertTrue(body.contains(">Expert<"));
    assertTrue(body.contains(">14/10/2026 au 30/09/2027<"));
    assertTrue(body.contains(">Mensuel<"));
    assertFalse(body.contains("Abonnement Expert du 14/10/2026 au 31/10/2026"));
    assertFalse(body.contains("31/10/2026"));
  }

  @Test
  void upgrade_invoice_mail_labels_a_yearly_coverage_as_annual() throws Exception {
    var summary =
        new SubscriptionMailSummary(
            "Pro", YEARLY, LocalDate.of(2026, 10, 14), LocalDate.of(2027, 10, 13));

    subject.sendWithSummary(
        upgradeInvoice(), summary, SUBSCRIPTION_INVOICE_MAIL_TEMPLATE, "s3d@client.fr");

    var body = sentHtmlBody();
    assertTrue(body.contains(">Annuel<"));
    assertTrue(body.contains(">14/10/2026 au 13/10/2027<"));
  }

  @Test
  void summary_mail_falls_back_on_the_default_label_and_the_payment_date() throws Exception {
    var summary = new SubscriptionMailSummary(null, null, null, null);

    subject.sendWithSummary(
        upgradeInvoice(), summary, SUBSCRIPTION_INVOICE_MAIL_TEMPLATE, "s3d@client.fr");

    var body = sentHtmlBody();
    assertTrue(body.contains(">" + SubscriptionPayment.DEFAULT_LABEL + "<"));
    assertTrue(body.contains(">14/10/2026<"));
    assertFalse(body.contains("Règlement"));
  }

  @Test
  void summary_mail_is_not_sent_without_recipient() throws Exception {
    var summary =
        new SubscriptionMailSummary(
            "Expert", MONTHLY, LocalDate.of(2026, 10, 14), LocalDate.of(2027, 9, 30));

    assertFalse(
        subject.sendWithSummary(
            upgradeInvoice(), summary, SUBSCRIPTION_INVOICE_MAIL_TEMPLATE, null));

    verify(mailer, never())
        .sendEmail(anyString(), anyString(), anyString(), anyString(), anyList());
  }

  @Test
  void payment_invoice_mail_keeps_the_payment_plan_and_the_invoiced_period() throws Exception {
    when(subscriptionInvoicePeriodRepository.findByInvoiceId("invoice_id"))
        .thenReturn(
            Optional.of(
                SubscriptionInvoicePeriod.builder()
                    .periodStartDatetime(
                        LocalDate.of(2026, 9, 24)
                            .atStartOfDay(ZoneId.of("Europe/Paris"))
                            .toInstant())
                    .periodEndDatetime(
                        LocalDate.of(2027, 8, 31)
                            .atStartOfDay(ZoneId.of("Europe/Paris"))
                            .toInstant())
                    .build()));

    subject.send(someInvoice(), somePayment(), SUBSCRIPTION_INVOICE_MAIL_TEMPLATE, "s3d@client.fr");

    var body = sentHtmlBody();
    assertTrue(body.contains(">Essentiel<"));
    assertTrue(body.contains(">24/09/2026 au 31/08/2027<"));
    assertTrue(body.contains(">Mensuel<"));
  }

  private String sentHtmlBody() throws Exception {
    var htmlBody = ArgumentCaptor.forClass(String.class);
    verify(mailer).sendEmail(anyString(), anyString(), anyString(), htmlBody.capture(), anyList());
    return htmlBody.getValue();
  }

  private Invoice upgradeInvoice() {
    return someInvoice().toBuilder()
        .ref("REF-14102026094820")
        .sendingDate(LocalDate.of(2026, 10, 14))
        .products(
            List.of(
                InvoiceProduct.builder()
                    .description("Abonnement Expert du 14/10/2026 au 31/10/2026")
                    .quantity(1)
                    .build()))
        .build();
  }

  private Invoice someInvoice() {
    return Invoice.builder()
        .id("invoice_id")
        .ref("REF-24092026155834")
        .fileId("file_id")
        .user(User.builder().id("admin_user_id").build())
        .customer(
            Customer.builder()
                .id("customer_id")
                .name("S3D Hygiène Professionnel")
                .email("s3d@client.fr")
                .build())
        .sendingDate(LocalDate.of(2026, 9, 24))
        .products(
            List.of(
                InvoiceProduct.builder().description("Abonnement Essentiel").quantity(1).build()))
        .totalPriceWithoutVat(new Fraction(BigInteger.valueOf(1035)))
        .totalPriceWithVat(new Fraction(BigInteger.valueOf(1242)))
        .build();
  }

  private SubscriptionPayment somePayment() {
    return SubscriptionPayment.builder()
        .id("subscription_payment_id")
        .subscriptionProduct(SubscriptionProduct.builder().name("Essentiel").build())
        .billingInterval(MONTHLY)
        .amountInCentsWithoutVat(1_035L)
        .amountInCentsWithVat(1_242L)
        .vatPercent(2_000L)
        .periodStartDatetime(Instant.parse("2026-09-24T00:00:00Z"))
        .periodEndDatetime(Instant.parse("2026-09-30T00:00:00Z"))
        .paymentDatetime(Instant.parse("2026-09-24T13:58:34Z"))
        .build();
  }
}
