package app.bpartners.api.service.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.SubscriptionPaymentInvoiceRequested;
import app.bpartners.api.endpoint.rest.model.CustomerType;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.User;
import app.bpartners.api.model.subscription.AnnualInvoiceBillingType;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.SubscriptionPayment;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.SubscriptionPaymentRepository;
import app.bpartners.api.service.customer.SubscriptionCustomerResolver;
import app.bpartners.api.service.invoice.InvoiceService;
import app.bpartners.api.service.subscription.SubscriptionPaymentService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import com.lowagie.text.DocumentException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

class SubscriptionPaymentInvoiceRequestedServicePreviewTest {
  private static final String ADMIN_USER_ID = "admin_user_id";
  private static final String PAYMENT_ID = "subscription_payment_id";
  private static final Instant PAID_AT = Instant.parse("2026-01-01T09:30:00Z");
  private static final Instant PERIOD_START = Instant.parse("2026-01-01T09:30:00Z");
  private static final Instant PERIOD_END = Instant.parse("2027-01-01T09:30:00Z");
  private static final Path PREVIEW_DIR = Path.of("build", "annual-invoice-previews");

  SubscriptionPaymentRepository subscriptionPaymentRepository = mock();
  SubscriptionPaymentService subscriptionPaymentService = mock();
  UserRepository userRepository = mock();
  UserSubscriptionConf userSubscriptionConf = mock();
  SubscriptionCustomerResolver subscriptionCustomerResolver = mock();
  InvoiceService invoiceService = mock();
  EventProducer eventProducer = mock();
  SubscriptionPaymentInvoiceRequestedService subject =
      new SubscriptionPaymentInvoiceRequestedService(
          subscriptionPaymentRepository,
          subscriptionPaymentService,
          userRepository,
          userSubscriptionConf,
          subscriptionCustomerResolver,
          invoiceService,
          new CustomDateFormatter(),
          eventProducer);

  SubscriptionPaymentInvoiceRequestedServicePreviewTest() {
    when(userSubscriptionConf.getUserToCreditId()).thenReturn(ADMIN_USER_ID);
    when(invoiceService.crupdateSubscriptionInvoice(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    var adminUser = adminUser();
    var subscriber = User.builder().id("subscriber_id").email("subscriber@email.com").build();
    when(userRepository.getById(ADMIN_USER_ID)).thenReturn(adminUser);
    when(userRepository.getById("subscriber_id")).thenReturn(subscriber);
    when(subscriptionCustomerResolver.apply(adminUser, subscriber))
        .thenReturn(subscriberCustomer());
    when(subscriptionPaymentRepository.findById(PAYMENT_ID))
        .thenReturn(Optional.of(yearlyPayment()));
  }

  @AfterEach
  void resetBillingType() {
    SubscriptionPaymentInvoiceRequestedService.annualInvoiceBillingType =
        AnnualInvoiceBillingType.MONTHLY_DETAILED;
  }

  @Test
  void renders_a_preview_for_the_single_monthly_line_variant()
      throws IOException, DocumentException {
    SubscriptionPaymentInvoiceRequestedService.annualInvoiceBillingType =
        AnnualInvoiceBillingType.MONTHLY_QUANTITY;

    var invoice = producedInvoice();
    var html = render(invoice);
    var previewFile = writePdf("annual-invoice-monthly-quantity.pdf", html);

    assertEquals(1, invoice.getProducts().size());
    assertEquals(12, invoice.getProducts().getFirst().getQuantity());
    assertTrue(html.contains("Abonnement mensuel"));
    assertTrue(html.contains("Remise"));
    System.out.println("Aperçu PDF (Abonnement mensuel x12) : " + previewFile.toAbsolutePath());
  }

  @Test
  void renders_a_preview_for_the_twelve_detailed_lines_variant()
      throws IOException, DocumentException {
    SubscriptionPaymentInvoiceRequestedService.annualInvoiceBillingType =
        AnnualInvoiceBillingType.MONTHLY_DETAILED;

    var invoice = producedInvoice();
    var html = render(invoice);
    var previewFile = writePdf("annual-invoice-monthly-detailed.pdf", html);

    assertEquals(12, invoice.getProducts().size());
    assertEquals(12, countOccurrences(html, "Abonnement mensuel du"));
    assertTrue(html.contains("Remise"));
    assertEquals(1, pdfPageCount(html));
    System.out.println(
        "Aperçu PDF (12 lignes mensuelles détaillées) : " + previewFile.toAbsolutePath());
  }

  private Invoice producedInvoice() {
    subject.accept(
        SubscriptionPaymentInvoiceRequested.builder().subscriptionPaymentId(PAYMENT_ID).build());
    var captor = ArgumentCaptor.forClass(Invoice.class);
    verify(invoiceService).crupdateSubscriptionInvoice(captor.capture());
    var invoice = captor.getValue();
    invoice.setUpdatedAt(PAID_AT);
    return invoice;
  }

  private String render(Invoice invoice) {
    var context = new Context();
    context.setVariable("invoice", invoice);
    context.setVariable("logo", null);
    context.setVariable("account", invoice.getActualAccount());
    context.setVariable("accountHolder", invoice.getActualHolder());
    return new TemplateResolverEngine().getTemplateEngine().process("invoice", context);
  }

  private Path writePdf(String fileName, String html) throws IOException, DocumentException {
    Files.createDirectories(PREVIEW_DIR);
    var file = PREVIEW_DIR.resolve(fileName);
    var renderer = new ITextRenderer();
    renderer.setDocumentFromString(html);
    renderer.layout();
    try (var outputStream = new ByteArrayOutputStream()) {
      renderer.createPDF(outputStream);
      Files.write(file, outputStream.toByteArray());
    }
    file.toFile().deleteOnExit();
    return file;
  }

  private int pdfPageCount(String html) throws DocumentException, IOException {
    var renderer = new ITextRenderer();
    renderer.setDocumentFromString(html);
    renderer.layout();
    try (var outputStream = new ByteArrayOutputStream()) {
      renderer.createPDF(outputStream);
      var content = new String(outputStream.toByteArray(), StandardCharsets.ISO_8859_1);
      var matcher = Pattern.compile("/Type\\s*/Page(?!s)").matcher(content);
      var count = 0;
      while (matcher.find()) {
        count++;
      }
      return count;
    }
  }

  private static int countOccurrences(String text, String token) {
    int count = 0;
    int index = text.indexOf(token);
    while (index >= 0) {
      count++;
      index = text.indexOf(token, index + token.length());
    }
    return count;
  }

  private User adminUser() {
    var account =
        Account.builder()
            .id("admin_account_id")
            .name("BPartners SAS")
            .iban("FR7630006000011234567890189")
            .bic("AGRIFRPP")
            .active(true)
            .build();
    var accountHolder =
        AccountHolder.builder()
            .name("BPartners SAS")
            .address("229 rue Saint-Honoré")
            .postalCode("75001")
            .city("Paris")
            .country("France")
            .socialCapital(1_000_000)
            .siren("123456789")
            .vatNumber("FR12345678901")
            .mobilePhoneNumber("+33100000000")
            .email("contact@bpartners.app")
            .website("https://bpartners.app")
            .subjectToVat(true)
            .build();
    return User.builder()
        .id(ADMIN_USER_ID)
        .accounts(List.of(account))
        .accountHolders(List.of(accountHolder))
        .build();
  }

  private Customer subscriberCustomer() {
    return Customer.builder()
        .id("customer_id")
        .name("Buyer SARL")
        .customerType(CustomerType.PROFESSIONAL)
        .email("subscriber@email.com")
        .phone("+33600000000")
        .address("10 avenue des Champs-Élysées")
        .zipCode(75008)
        .city("Paris")
        .country("France")
        .build();
  }

  private SubscriptionPayment yearlyPayment() {
    return SubscriptionPayment.builder()
        .id(PAYMENT_ID)
        .userId("subscriber_id")
        .stripeInvoiceId("in_123")
        .label("Essentiel")
        .billingInterval(BillingInterval.YEARLY)
        .subscriptionProduct(
            SubscriptionProduct.builder()
                .name("Essentiel")
                .annualDiscountPercent(1000)
                .priceInCentsWithoutVat(4_900L)
                .build())
        .amountInCentsWithoutVat(52_900L)
        .amountInCentsWithVat(63_480L)
        .vatPercent(2_000L)
        .periodStartDatetime(PERIOD_START)
        .periodEndDatetime(PERIOD_END)
        .paymentDatetime(PAID_AT)
        .build();
  }
}
