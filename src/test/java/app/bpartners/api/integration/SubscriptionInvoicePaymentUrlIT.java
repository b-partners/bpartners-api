package app.bpartners.api.integration;

import static app.bpartners.api.endpoint.rest.model.PaymentStatus.UNPAID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.mapper.SubscriptionInvoiceRestMapper;
import app.bpartners.api.endpoint.rest.model.SubscriptionInvoice;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.User;
import app.bpartners.api.service.aws.S3Service;
import app.bpartners.api.service.subscription.StripeInvoiceService;
import app.bpartners.api.service.subscription.SubscriptionInvoiceService;
import app.bpartners.api.service.user.UserService;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
class SubscriptionInvoicePaymentUrlIT extends MockedThirdParties {
  private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
  private static final String USER_TO_CREDIT_ID = "user_to_credit_id";
  private static final String SUBSCRIBER_ID = "subscriber_id";
  private static final String SUBSCRIBER_EMAIL = "subscriber@email.com";
  private static final String STRIPE_CUSTOMER_ID = "cus_subscriber_test";
  private static final String MARCH_INVOICE_ID = "subscription_invoice_march_id";
  private static final String FEBRUARY_INVOICE_ID = "subscription_invoice_february_id";
  private static final String MARCH_HOSTED_URL = "https://stripe/hosted/march";
  private static final String FEBRUARY_HOSTED_URL = "https://stripe/hosted/february";
  private static final YearMonth MARCH_2024 = YearMonth.of(2024, 3);
  private static final YearMonth FEBRUARY_2024 = YearMonth.of(2024, 2);

  @Autowired SubscriptionInvoiceRestMapper subject;
  @Autowired SubscriptionInvoiceService subscriptionInvoiceService;
  @MockBean StripeInvoiceService stripeInvoiceService;
  @MockBean S3Service s3Service;
  @MockBean UserService userService;

  @BeforeEach
  void setUp() {
    when(userSubscriptionConf.getUserToCreditId()).thenReturn(USER_TO_CREDIT_ID);
    when(s3Service.presignURL(any(), any(), any(), anyLong())).thenReturn("dummy_presigned_url");
    when(userService.getUserById(SUBSCRIBER_ID))
        .thenReturn(
            User.builder()
                .id(SUBSCRIBER_ID)
                .email(SUBSCRIBER_EMAIL)
                .userSubscriptionId(STRIPE_CUSTOMER_ID)
                .build());
  }

  @Test
  void two_unpaid_invoices_same_amount_are_matched_to_stripe_by_period() {
    var march =
        onlyInvoice(subscriptionInvoiceService.getSubscriptionInvoices(SUBSCRIBER_ID, MARCH_2024));
    var february =
        onlyInvoice(
            subscriptionInvoiceService.getSubscriptionInvoices(SUBSCRIBER_ID, FEBRUARY_2024));
    var totalInCents = totalInCentsOf(march);
    when(stripeInvoiceService.getUnpaidStripeInvoices(STRIPE_CUSTOMER_ID))
        .thenReturn(
            List.of(
                stripeInvoice(totalInCents, MARCH_HOSTED_URL, MARCH_2024),
                stripeInvoice(totalInCents, FEBRUARY_HOSTED_URL, FEBRUARY_2024)));

    var actual = subject.toRest(SUBSCRIBER_ID, List.of(march, february));

    var paymentUrlByInvoiceId = paymentUrlByInvoiceId(actual);
    assertEquals(
        Map.of(
            MARCH_INVOICE_ID, MARCH_HOSTED_URL,
            FEBRUARY_INVOICE_ID, FEBRUARY_HOSTED_URL),
        paymentUrlByInvoiceId);
    assertEquals(UNPAID, paymentStatusOf(actual, MARCH_INVOICE_ID));
    assertEquals(UNPAID, paymentStatusOf(actual, FEBRUARY_INVOICE_ID));
  }

  @Test
  void a_single_stripe_invoice_is_assigned_to_the_closest_period_and_not_reused() {
    var march =
        onlyInvoice(subscriptionInvoiceService.getSubscriptionInvoices(SUBSCRIBER_ID, MARCH_2024));
    var february =
        onlyInvoice(
            subscriptionInvoiceService.getSubscriptionInvoices(SUBSCRIBER_ID, FEBRUARY_2024));
    var totalInCents = totalInCentsOf(march);
    when(stripeInvoiceService.getUnpaidStripeInvoices(STRIPE_CUSTOMER_ID))
        .thenReturn(List.of(stripeInvoice(totalInCents, MARCH_HOSTED_URL, MARCH_2024)));

    var actual = subject.toRest(SUBSCRIBER_ID, List.of(february, march));

    assertEquals(Map.of(MARCH_INVOICE_ID, MARCH_HOSTED_URL), paymentUrlByInvoiceId(actual));
  }

  private static Invoice onlyInvoice(List<Invoice> invoices) {
    assertEquals(1, invoices.size());
    return invoices.getFirst();
  }

  private static Long totalInCentsOf(Invoice invoice) {
    return invoice.getTotalPriceWithVat() == null
        ? null
        : Long.valueOf(invoice.getTotalPriceWithVat().getCentsRoundUp());
  }

  private static Map<String, String> paymentUrlByInvoiceId(List<SubscriptionInvoice> invoices) {
    return invoices.stream()
        .filter(subscriptionInvoice -> subscriptionInvoice.getPaymentUrl() != null)
        .collect(
            Collectors.toMap(
                subscriptionInvoice -> subscriptionInvoice.getInvoice().getId(),
                SubscriptionInvoice::getPaymentUrl));
  }

  private static app.bpartners.api.endpoint.rest.model.PaymentStatus paymentStatusOf(
      List<SubscriptionInvoice> invoices, String invoiceId) {
    return invoices.stream()
        .collect(Collectors.toMap(i -> i.getInvoice().getId(), Function.identity()))
        .get(invoiceId)
        .getPaymentStatus();
  }

  private static com.stripe.model.Invoice stripeInvoice(
      Long total, String hostedInvoiceUrl, YearMonth period) {
    var stripeInvoice = new com.stripe.model.Invoice();
    stripeInvoice.setTotal(total);
    stripeInvoice.setHostedInvoiceUrl(hostedInvoiceUrl);
    stripeInvoice.setPeriodEnd(
        period.atEndOfMonth().atStartOfDay(PARIS).toInstant().getEpochSecond());
    return stripeInvoice;
  }
}
