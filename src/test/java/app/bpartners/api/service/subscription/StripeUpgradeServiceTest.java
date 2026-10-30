package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static app.bpartners.api.model.subscription.BillingInterval.YEARLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import com.stripe.StripeClient;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceItem;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.InvoiceCreateParams;
import com.stripe.param.InvoiceItemCreateParams;
import com.stripe.param.InvoicePayParams;
import com.stripe.param.InvoiceRetrieveParams;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.service.InvoiceItemService;
import com.stripe.service.InvoiceService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class StripeUpgradeServiceTest {
  private static final String CUSTOMER_ID = "cus_1";
  private static final String PAYMENT_METHOD_ID = "pm_1";
  private static final String UPGRADE_ID = "upgrade_id";
  private static final String HOSTED_URL = "https://invoice.stripe.com/i/in_1";

  StripeClient stripeClient = mock();
  StripeDefaultPaymentMethodService defaultPaymentMethodService = mock();
  InvoiceService invoiceService = mock();
  InvoiceItemService invoiceItemService = mock();
  com.stripe.service.SubscriptionService subscriptionService = mock();
  StripeUpgradeService subject =
      new StripeUpgradeService(stripeClient, defaultPaymentMethodService);

  @BeforeEach
  void setUp() throws Exception {
    when(stripeClient.invoices()).thenReturn(invoiceService);
    when(stripeClient.invoiceItems()).thenReturn(invoiceItemService);
    when(stripeClient.subscriptions()).thenReturn(subscriptionService);
    when(invoiceService.create(any(InvoiceCreateParams.class), any(RequestOptions.class)))
        .thenReturn(stripeInvoice("in_1", "draft"));
    when(invoiceItemService.create(any(InvoiceItemCreateParams.class), any(RequestOptions.class)))
        .thenReturn(new InvoiceItem());
    when(invoiceService.finalizeInvoice("in_1")).thenReturn(finalizedInvoice(HOSTED_URL));
  }

  @Test
  void requires_a_default_payment_method() {
    when(defaultPaymentMethodService.ensureDefaultPaymentMethod(CUSTOMER_ID))
        .thenReturn(Optional.of(PAYMENT_METHOD_ID));

    assertEquals(PAYMENT_METHOD_ID, subject.requireDefaultPaymentMethod(CUSTOMER_ID));
  }

  @Test
  void refuses_a_customer_without_payment_method() {
    when(defaultPaymentMethodService.ensureDefaultPaymentMethod(CUSTOMER_ID))
        .thenReturn(Optional.empty());

    assertThrows(BadRequestException.class, () -> subject.requireDefaultPaymentMethod(CUSTOMER_ID));
  }

  @Test
  void charges_the_amount_on_a_standalone_invoice_and_returns_its_identifier() throws Exception {
    when(invoiceService.pay(eq("in_1"), any(InvoicePayParams.class)))
        .thenReturn(stripeInvoice("in_1", "paid"));

    var actual =
        subject.chargeOneOffAmount(CUSTOMER_ID, PAYMENT_METHOD_ID, 3_871L, "label", UPGRADE_ID);

    assertEquals(new UpgradeCharge.Paid("in_1"), actual);
    var item = ArgumentCaptor.forClass(InvoiceItemCreateParams.class);
    verify(invoiceItemService).create(item.capture(), any(RequestOptions.class));
    assertEquals(3_871L, item.getValue().getAmount());
    assertEquals("in_1", item.getValue().getInvoice());
    verify(invoiceService).finalizeInvoice("in_1");
    verify(invoiceService, never()).voidInvoice(anyString());
  }

  @Test
  void voids_the_invoice_when_it_is_not_paid_after_the_payment_attempt() throws Exception {
    when(invoiceService.pay(eq("in_1"), any(InvoicePayParams.class)))
        .thenReturn(stripeInvoice("in_1", "open"));

    assertThrows(
        BadRequestException.class,
        () ->
            subject.chargeOneOffAmount(CUSTOMER_ID, PAYMENT_METHOD_ID, 100L, "label", UPGRADE_ID));

    verify(invoiceService).voidInvoice("in_1");
  }

  @Test
  void voids_the_invoice_when_the_payment_fails() throws Exception {
    when(invoiceService.pay(eq("in_1"), any(InvoicePayParams.class))).thenThrow(stripeException());

    assertThrows(
        BadRequestException.class,
        () ->
            subject.chargeOneOffAmount(CUSTOMER_ID, PAYMENT_METHOD_ID, 100L, "label", UPGRADE_ID));

    verify(invoiceService).voidInvoice("in_1");
  }

  @Test
  void keeps_the_invoice_open_and_exposes_its_payment_page_when_the_bank_declines_the_card()
      throws Exception {
    when(invoiceService.pay(eq("in_1"), any(InvoicePayParams.class))).thenThrow(cardException());

    var actual =
        subject.chargeOneOffAmount(CUSTOMER_ID, PAYMENT_METHOD_ID, 100L, "label", UPGRADE_ID);

    assertEquals(new UpgradeCharge.ActionRequired("in_1", HOSTED_URL), actual);
    verify(invoiceService, never()).voidInvoice(anyString());
  }

  @Test
  void keeps_the_invoice_open_when_the_payment_requires_an_action_of_the_customer()
      throws Exception {
    when(invoiceService.pay(eq("in_1"), any(InvoicePayParams.class)))
        .thenThrow(
            new com.stripe.exception.ApiException(
                "requires action", "req_1", "invoice_payment_intent_requires_action", 402, null));

    var actual =
        subject.chargeOneOffAmount(CUSTOMER_ID, PAYMENT_METHOD_ID, 100L, "label", UPGRADE_ID);

    assertEquals(new UpgradeCharge.ActionRequired("in_1", HOSTED_URL), actual);
    verify(invoiceService, never()).voidInvoice(anyString());
  }

  @Test
  void voids_the_invoice_when_the_customer_has_no_payment_page_to_pay_on() throws Exception {
    when(invoiceService.finalizeInvoice("in_1")).thenReturn(finalizedInvoice(null));
    when(invoiceService.pay(eq("in_1"), any(InvoicePayParams.class))).thenThrow(cardException());

    assertThrows(
        BadRequestException.class,
        () ->
            subject.chargeOneOffAmount(CUSTOMER_ID, PAYMENT_METHOD_ID, 100L, "label", UPGRADE_ID));

    verify(invoiceService).voidInvoice("in_1");
  }

  @Test
  void voids_an_open_invoice() throws Exception {
    assertEquals(StripeInvoiceVoidOutcome.VOIDED, subject.voidOpenInvoice("in_1"));

    verify(invoiceService).voidInvoice("in_1");
  }

  @Test
  void reports_an_invoice_paid_in_the_meantime_instead_of_voiding_it() throws Exception {
    when(invoiceService.voidInvoice("in_1")).thenThrow(stripeException());
    when(invoiceService.retrieve("in_1")).thenReturn(stripeInvoice("in_1", "paid"));

    assertEquals(StripeInvoiceVoidOutcome.PAID, subject.voidOpenInvoice("in_1"));
  }

  @Test
  void considers_an_already_void_invoice_as_voided() throws Exception {
    when(invoiceService.voidInvoice("in_1")).thenThrow(stripeException());
    when(invoiceService.retrieve("in_1")).thenReturn(stripeInvoice("in_1", "void"));

    assertEquals(StripeInvoiceVoidOutcome.VOIDED, subject.voidOpenInvoice("in_1"));
  }

  @Test
  void fails_when_an_open_invoice_can_not_be_voided() throws Exception {
    when(invoiceService.voidInvoice("in_1")).thenThrow(stripeException());
    when(invoiceService.retrieve("in_1")).thenReturn(stripeInvoice("in_1", "open"));

    assertThrows(ApiException.class, () -> subject.voidOpenInvoice("in_1"));
  }

  @Test
  void fails_when_the_invoice_status_can_not_be_read_after_a_failed_void() throws Exception {
    when(invoiceService.voidInvoice("in_1")).thenThrow(stripeException());
    when(invoiceService.retrieve("in_1")).thenThrow(stripeException());

    assertThrows(ApiException.class, () -> subject.voidOpenInvoice("in_1"));
  }

  @Test
  void resolves_the_payment_method_the_customer_really_paid_with() throws Exception {
    var paymentIntent = new PaymentIntent();
    paymentIntent.setPaymentMethod("pm_new_card");
    var invoice = stripeInvoice("in_1", "paid");
    invoice.setPaymentIntentObject(paymentIntent);
    when(invoiceService.retrieve(eq("in_1"), any(InvoiceRetrieveParams.class))).thenReturn(invoice);

    assertEquals("pm_new_card", subject.resolveChargedPaymentMethod(CUSTOMER_ID, "in_1"));

    var params = ArgumentCaptor.forClass(InvoiceRetrieveParams.class);
    verify(invoiceService).retrieve(eq("in_1"), params.capture());
    assertEquals(List.of("payment_intent"), params.getValue().getExpand());
  }

  @Test
  void falls_back_on_the_default_payment_method_when_the_paid_invoice_names_none()
      throws Exception {
    when(invoiceService.retrieve(eq("in_1"), any(InvoiceRetrieveParams.class)))
        .thenReturn(stripeInvoice("in_1", "paid"));
    when(defaultPaymentMethodService.ensureDefaultPaymentMethod(CUSTOMER_ID))
        .thenReturn(Optional.of(PAYMENT_METHOD_ID));

    assertEquals(PAYMENT_METHOD_ID, subject.resolveChargedPaymentMethod(CUSTOMER_ID, "in_1"));
  }

  @Test
  void fails_when_the_paid_invoice_can_not_be_retrieved() throws Exception {
    when(invoiceService.retrieve(eq("in_1"), any(InvoiceRetrieveParams.class)))
        .thenThrow(stripeException());

    assertThrows(
        ApiException.class, () -> subject.resolveChargedPaymentMethod(CUSTOMER_ID, "in_1"));
  }

  @Test
  void keeps_failing_with_a_bad_request_when_the_void_fails_too() throws Exception {
    when(invoiceService.pay(eq("in_1"), any(InvoicePayParams.class))).thenThrow(stripeException());
    when(invoiceService.voidInvoice("in_1")).thenThrow(stripeException());

    assertThrows(
        BadRequestException.class,
        () ->
            subject.chargeOneOffAmount(CUSTOMER_ID, PAYMENT_METHOD_ID, 100L, "label", UPGRADE_ID));
  }

  @Test
  void does_not_void_anything_when_the_invoice_could_not_even_be_created() throws Exception {
    when(invoiceService.create(any(InvoiceCreateParams.class), any(RequestOptions.class)))
        .thenThrow(stripeException());

    assertThrows(
        BadRequestException.class,
        () ->
            subject.chargeOneOffAmount(CUSTOMER_ID, PAYMENT_METHOD_ID, 100L, "label", UPGRADE_ID));

    verify(invoiceService, never()).voidInvoice(anyString());
  }

  @Test
  void creates_a_monthly_subscription_from_inline_price_data_anchored_without_proration()
      throws Exception {
    when(subscriptionService.create(any(SubscriptionCreateParams.class), any(RequestOptions.class)))
        .thenReturn(stripeSubscription("sub_new"));

    var actual =
        subject.createUpgradedSubscription(
            CUSTOMER_ID, PAYMENT_METHOD_ID, product(), MONTHLY, 1_793_000_000L, UPGRADE_ID);

    assertEquals("sub_new", actual);
    var params = ArgumentCaptor.forClass(SubscriptionCreateParams.class);
    verify(subscriptionService).create(params.capture(), any(RequestOptions.class));
    var item = params.getValue().getItems().getFirst();
    assertEquals(11_880L, item.getPriceData().getUnitAmount());
    assertEquals("prod_e2", item.getPriceData().getProduct());
    assertEquals(1_793_000_000L, params.getValue().getBillingCycleAnchor());
    assertEquals(
        SubscriptionCreateParams.ProrationBehavior.NONE, params.getValue().getProrationBehavior());
  }

  @Test
  void creates_a_yearly_subscription_from_the_annual_price() throws Exception {
    when(subscriptionService.create(any(SubscriptionCreateParams.class), any(RequestOptions.class)))
        .thenReturn(stripeSubscription("sub_new"));

    subject.createUpgradedSubscription(
        CUSTOMER_ID, PAYMENT_METHOD_ID, product(), YEARLY, 1_793_000_000L, UPGRADE_ID);

    var params = ArgumentCaptor.forClass(SubscriptionCreateParams.class);
    verify(subscriptionService).create(params.capture(), any(RequestOptions.class));
    assertEquals("price_annual", params.getValue().getItems().getFirst().getPrice());
  }

  @Test
  void wraps_a_stripe_failure_when_creating_the_subscription() throws Exception {
    when(subscriptionService.create(any(SubscriptionCreateParams.class), any(RequestOptions.class)))
        .thenThrow(stripeException());

    var plan = product();

    assertThrows(
        ApiException.class,
        () ->
            subject.createUpgradedSubscription(
                CUSTOMER_ID, PAYMENT_METHOD_ID, plan, MONTHLY, 1L, UPGRADE_ID));
  }

  private static Invoice stripeInvoice(String id, String status) {
    var invoice = new Invoice();
    invoice.setId(id);
    invoice.setStatus(status);
    return invoice;
  }

  private static Invoice finalizedInvoice(String hostedInvoiceUrl) {
    var invoice = stripeInvoice("in_1", "open");
    invoice.setHostedInvoiceUrl(hostedInvoiceUrl);
    return invoice;
  }

  private static com.stripe.exception.CardException cardException() {
    return new com.stripe.exception.CardException(
        "Your card was declined.",
        "req_1",
        "card_declined",
        null,
        "generic_decline",
        null,
        402,
        null);
  }

  private static com.stripe.model.Subscription stripeSubscription(String id) {
    var subscription = new com.stripe.model.Subscription();
    subscription.setId(id);
    return subscription;
  }

  private static com.stripe.exception.StripeException stripeException() {
    return new com.stripe.exception.ApiException("boom", "req_1", "code", 500, null);
  }

  private static SubscriptionProduct product() {
    return SubscriptionProduct.builder()
        .id("pro_id")
        .e2Id("prod_e2")
        .vatPercent(2_000L)
        .priceInCentsWithoutVat(9_900L)
        .annualE2PriceId("price_annual")
        .annualPriceInCentsWithVat(128_280L)
        .build();
  }
}
