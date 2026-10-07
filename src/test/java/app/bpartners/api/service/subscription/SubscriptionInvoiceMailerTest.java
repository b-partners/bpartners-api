package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static app.bpartners.api.service.subscription.SubscriptionInvoiceMailer.SUBSCRIPTION_INVOICE_MAIL_TEMPLATE;
import static app.bpartners.api.service.subscription.SubscriptionInvoiceMailer.TECH_RECIPIENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.file.FileWriter;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceProduct;
import app.bpartners.api.model.User;
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
