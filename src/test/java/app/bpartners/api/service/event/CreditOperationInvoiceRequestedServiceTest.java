package app.bpartners.api.service.event;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.model.credit.CreditPurchaseStatus.COMPLETED;
import static app.bpartners.api.model.credit.CreditPurchaseStatus.PENDING;
import static app.bpartners.api.model.credit.CreditPurchaseType.CUSTOM;
import static app.bpartners.api.model.credit.CreditPurchaseType.PACK;
import static app.bpartners.api.model.credit.CreditTransactionMovementType.CREDIT;
import static app.bpartners.api.model.credit.CreditTransactionMovementType.DEBIT;
import static app.bpartners.api.model.credit.CreditTransactionType.PURCHASE;
import static app.bpartners.api.model.credit.CreditTransactionType.PURCHASE_REFUND;
import static app.bpartners.api.model.credit.CreditTransactionType.SUBSCRIPTION_GRANT;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.CreditOperationInvoiceCreated;
import app.bpartners.api.endpoint.event.model.CreditOperationInvoiceRequested;
import app.bpartners.api.endpoint.rest.model.ArchiveStatus;
import app.bpartners.api.endpoint.rest.model.Invoice.PaymentTypeEnum;
import app.bpartners.api.endpoint.rest.model.PaymentMethod;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.User;
import app.bpartners.api.model.credit.CreditPack;
import app.bpartners.api.model.credit.CreditPurchase;
import app.bpartners.api.model.credit.CreditTransaction;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.CreditPurchaseRepository;
import app.bpartners.api.service.customer.SubscriptionCustomerResolver;
import app.bpartners.api.service.invoice.InvoiceService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class CreditOperationInvoiceRequestedServiceTest {
  private static final String ADMIN_USER_ID = "admin_user_id";
  private static final Instant COMPLETED_AT = Instant.parse("2026-03-04T09:30:00Z");

  CreditPurchaseRepository creditPurchaseRepository = mock();
  UserRepository userRepository = mock();
  UserSubscriptionConf userSubscriptionConf = mock();
  SubscriptionCustomerResolver subscriptionCustomerResolver = mock();
  InvoiceService invoiceService = mock();
  EventProducer eventProducer = mock();
  CreditOperationInvoiceRequestedService subject =
      new CreditOperationInvoiceRequestedService(
          creditPurchaseRepository,
          userRepository,
          userSubscriptionConf,
          subscriptionCustomerResolver,
          invoiceService,
          new CustomDateFormatter(),
          eventProducer);

  CreditOperationInvoiceRequestedServiceTest() {
    when(userSubscriptionConf.getUserToCreditId()).thenReturn(ADMIN_USER_ID);
    when(invoiceService.crupdateSubscriptionInvoice(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void generates_an_admin_issued_subscription_invoice_for_the_buyer() {
    var adminUser = User.builder().id(ADMIN_USER_ID).build();
    var buyer = User.builder().id("buyer_id").email("buyer@email.com").build();
    var buyerAsCustomer = Customer.builder().id("customer_id").name("Buyer SARL").build();
    givenUsers(adminUser, buyer);
    when(subscriptionCustomerResolver.apply(adminUser, buyer)).thenReturn(buyerAsCustomer);
    givenPurchase(somePackPurchase().build());

    subject.accept(somePurchaseEvent(30L));

    var invoice = capturedInvoice();
    assertEquals(adminUser, invoice.getUser());
    assertEquals(buyerAsCustomer, invoice.getCustomer());
    assertTrue(invoice.isSubscriptionInvoice());
    assertEquals(PAID, invoice.getStatus());
    assertEquals(ArchiveStatus.ENABLED, invoice.getArchiveStatus());
    assertEquals(PaymentMethod.CREDIT_CARD, invoice.getPaymentMethod());
    assertEquals(PaymentTypeEnum.CASH, invoice.getPaymentType());
    assertNotNull(invoice.getRef());
  }

  @Test
  void issues_a_paid_invoice_without_validity_date_for_a_completed_purchase() {
    givenDefaultUsersAndCustomer();
    givenPurchase(somePackPurchase().build());

    subject.accept(somePurchaseEvent(30L));

    var invoice = capturedInvoice();
    assertEquals(PAID, invoice.getStatus());
    assertNull(invoice.getValidityDate());
  }

  @Test
  void issues_an_invoice_awaiting_payment_for_a_purchase_not_completed_yet() {
    givenDefaultUsersAndCustomer();
    givenPurchase(somePackPurchase().status(PENDING).build());

    subject.accept(somePurchaseEvent(30L));

    var invoice = capturedInvoice();
    assertEquals(CONFIRMED, invoice.getStatus());
    assertEquals(LocalDate.of(2026, 4, 3), invoice.getValidityDate());
  }

  @Test
  void bills_a_pack_purchase_as_a_single_line_priced_for_the_whole_pack() {
    givenDefaultUsersAndCustomer();
    givenPurchase(somePackPurchase().build());

    subject.accept(somePurchaseEvent(30L));

    var invoice = capturedInvoice();
    assertEquals(1, invoice.getProducts().size());
    var product = invoice.getProducts().getFirst();
    assertEquals("Pack de 30 crédits d'analyse", product.getDescription());
    assertEquals(1, product.getQuantity());
    assertEquals(parseFraction(3000), product.getUnitPrice());
    assertEquals(parseFraction(2000), product.getVatPercent());
    assertEquals(invoice.getId(), product.getIdInvoice());
    assertEquals(parseFraction(3000), invoice.getTotalPriceWithoutDiscount());
    assertEquals(parseFraction(3000), invoice.getTotalPriceWithoutVat());
    assertEquals(parseFraction(600), invoice.getTotalVat());
    assertEquals(parseFraction(3600), invoice.getTotalPriceWithVat());
  }

  @Test
  void bills_a_multi_pack_purchase_for_the_amount_actually_charged() {
    givenDefaultUsersAndCustomer();
    var multiPackPurchase =
        somePackPurchase()
            .creditPack(CreditPack.builder().id("pack_id").description("Pack 100 crédits").build())
            .quantity(2)
            .credits(200L)
            .creditUnitPriceInCentsWithoutVat(250L)
            .amountInCentsWithoutVat(50_000L)
            .amountInCentsWithVat(60_000L)
            .build();
    givenPurchase(multiPackPurchase);

    subject.accept(somePurchaseEvent(200L));

    var invoice = capturedInvoice();
    var product = invoice.getProducts().getFirst();
    assertEquals(1, invoice.getProducts().size());
    assertEquals("Pack de 100 crédits d'analyse", product.getDescription());
    assertEquals(2, product.getQuantity());
    assertEquals(parseFraction(25_000), product.getUnitPrice());
    assertEquals(
        multiPackPurchase.getAmountInCentsWithoutVat().longValue(),
        invoice.getTotalPriceWithoutVat().getCentsRoundUp().longValue());
    assertEquals(
        multiPackPurchase.getAmountInCentsWithVat().longValue(),
        invoice.getTotalPriceWithVat().getCentsRoundUp().longValue());
  }

  @Test
  void dates_the_invoice_on_the_purchase_completion_day() {
    givenDefaultUsersAndCustomer();
    givenPurchase(somePackPurchase().build());

    subject.accept(somePurchaseEvent(30L));

    var invoice = capturedInvoice();
    assertEquals(LocalDate.of(2026, 3, 4), invoice.getSendingDate());
    assertEquals(LocalDate.of(2026, 3, 4), invoice.getToPayAt());
    assertEquals("Facture achat de crédits du 04/03/2026", invoice.getTitle());
    assertEquals(0, invoice.getDelayInPaymentAllowed());
  }

  @Test
  void falls_back_on_the_transaction_datetime_when_the_purchase_is_not_dated() {
    givenDefaultUsersAndCustomer();
    givenPurchase(somePackPurchase().completionDatetime(null).build());

    subject.accept(
        CreditOperationInvoiceRequested.builder()
            .creditTransaction(somePurchaseTransaction(30L).creationDatetime(COMPLETED_AT).build())
            .build());

    assertEquals(LocalDate.of(2026, 3, 4), capturedInvoice().getSendingDate());
  }

  @Test
  void dates_the_invoice_of_today_when_nothing_is_dated() {
    givenDefaultUsersAndCustomer();
    givenPurchase(somePackPurchase().completionDatetime(null).build());

    subject.accept(
        CreditOperationInvoiceRequested.builder()
            .creditTransaction(somePurchaseTransaction(30L).creationDatetime(null).build())
            .build());

    assertEquals(
        Instant.now().atZone(ZoneId.of("Europe/Paris")).toLocalDate(),
        capturedInvoice().getSendingDate());
  }

  @Test
  void retries_the_invoice_generation_for_at_most_five_minutes() {
    var event = somePurchaseEvent(30L);

    assertEquals(Duration.ofMinutes(5L), event.maxConsumerDuration());
    assertEquals(Duration.ofMinutes(1L), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void links_the_created_invoice_back_to_the_purchase() {
    givenDefaultUsersAndCustomer();
    givenPurchase(somePackPurchase().build());

    subject.accept(somePurchaseEvent(30L));

    var purchaseCaptor = ArgumentCaptor.forClass(CreditPurchase.class);
    verify(creditPurchaseRepository).save(purchaseCaptor.capture());
    assertEquals(capturedInvoice().getId(), purchaseCaptor.getValue().getInvoiceId());
  }

  @Test
  void requests_the_invoice_mail_once_the_invoice_is_created() {
    givenDefaultUsersAndCustomer();
    givenPurchase(somePackPurchase().build());

    subject.accept(somePurchaseEvent(30L));

    var inOrder = Mockito.inOrder(invoiceService, creditPurchaseRepository, eventProducer);
    inOrder.verify(invoiceService).crupdateSubscriptionInvoice(any());
    inOrder.verify(creditPurchaseRepository).save(any());
    inOrder.verify(eventProducer).accept(any());
    var requested = requestedMail();
    assertEquals(capturedInvoice().getId(), requested.getInvoiceId());
    assertEquals("purchase_id", requested.getCreditPurchaseId());
  }

  @Test
  void requests_no_invoice_mail_when_no_invoice_is_generated() {
    givenDefaultUsersAndCustomer();
    givenPurchase(somePackPurchase().invoiceId("already_invoiced").build());

    subject.accept(somePurchaseEvent(30L));

    verify(eventProducer, never()).accept(any());
  }

  @Test
  void generates_nothing_when_the_purchase_is_already_invoiced() {
    givenDefaultUsersAndCustomer();
    givenPurchase(somePackPurchase().invoiceId("already_invoiced").build());

    subject.accept(somePurchaseEvent(30L));

    verify(invoiceService, never()).crupdateSubscriptionInvoice(any());
    verify(creditPurchaseRepository, never()).save(any());
  }

  @Test
  void generates_nothing_when_the_purchase_is_gone() {
    when(creditPurchaseRepository.findById("purchase_id")).thenReturn(Optional.empty());

    subject.accept(somePurchaseEvent(30L));

    verify(invoiceService, never()).crupdateSubscriptionInvoice(any());
    verify(creditPurchaseRepository, never()).save(any());
  }

  @Test
  void generates_nothing_for_a_purchase_transaction_without_purchase_reference() {
    subject.accept(
        CreditOperationInvoiceRequested.builder()
            .creditTransaction(somePurchaseTransaction(30L).creditPurchaseId(null).build())
            .build());

    verify(creditPurchaseRepository, never()).findById(any());
    verify(invoiceService, never()).crupdateSubscriptionInvoice(any());
  }

  @Test
  void generates_nothing_for_a_subscription_grant() {
    subject.accept(
        CreditOperationInvoiceRequested.builder()
            .creditTransaction(
                CreditTransaction.builder()
                    .id("transaction_id")
                    .userId("buyer_id")
                    .type(SUBSCRIPTION_GRANT)
                    .movementType(CREDIT)
                    .credits(30L)
                    .creditPurchaseId("purchase_id")
                    .build())
            .build());

    verify(creditPurchaseRepository, never()).findById(any());
    verify(invoiceService, never()).crupdateSubscriptionInvoice(any());
  }

  @Test
  void generates_nothing_for_a_purchase_refund() {
    subject.accept(
        CreditOperationInvoiceRequested.builder()
            .creditTransaction(
                CreditTransaction.builder()
                    .id("transaction_id")
                    .userId("buyer_id")
                    .type(PURCHASE_REFUND)
                    .movementType(DEBIT)
                    .credits(30L)
                    .creditPurchaseId("purchase_id")
                    .build())
            .build());

    verify(creditPurchaseRepository, never()).findById(any());
    verify(invoiceService, never()).crupdateSubscriptionInvoice(any());
  }

  @Test
  void generates_nothing_without_credit_transaction() {
    subject.accept(CreditOperationInvoiceRequested.builder().build());

    verify(creditPurchaseRepository, never()).findById(any());
    verify(invoiceService, never()).crupdateSubscriptionInvoice(any());
  }

  @Test
  void describes_a_custom_purchase_without_repeating_the_credits_amount() {
    givenDefaultUsersAndCustomer();
    givenPurchase(somePackPurchase().type(CUSTOM).creditPack(null).quantity(null).build());

    subject.accept(somePurchaseEvent(30L));

    assertEquals(
        "Crédits d'analyse à l'unité", capturedInvoice().getProducts().getFirst().getDescription());
  }

  @Test
  void describes_a_pack_purchase_by_its_pack_credits_when_it_carries_no_pack_anymore() {
    givenDefaultUsersAndCustomer();
    givenPurchase(somePackPurchase().creditPack(null).build());

    subject.accept(somePurchaseEvent(30L));

    assertEquals(
        "Pack de 30 crédits d'analyse",
        capturedInvoice().getProducts().getFirst().getDescription());
  }

  private CreditOperationInvoiceCreated requestedMail() {
    var eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(eventsCaptor.capture());
    return (CreditOperationInvoiceCreated) eventsCaptor.getValue().getFirst();
  }

  private Invoice capturedInvoice() {
    var invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
    verify(invoiceService).crupdateSubscriptionInvoice(invoiceCaptor.capture());
    return invoiceCaptor.getValue();
  }

  private void givenDefaultUsersAndCustomer() {
    var adminUser = User.builder().id(ADMIN_USER_ID).build();
    var buyer = User.builder().id("buyer_id").email("buyer@email.com").build();
    givenUsers(adminUser, buyer);
    when(subscriptionCustomerResolver.apply(adminUser, buyer))
        .thenReturn(Customer.builder().id("customer_id").name("Buyer SARL").build());
  }

  private void givenUsers(User adminUser, User buyer) {
    when(userRepository.getById(ADMIN_USER_ID)).thenReturn(adminUser);
    when(userRepository.getById("buyer_id")).thenReturn(buyer);
  }

  private void givenPurchase(CreditPurchase creditPurchase) {
    when(creditPurchaseRepository.findById("purchase_id")).thenReturn(Optional.of(creditPurchase));
  }

  private CreditPurchase.CreditPurchaseBuilder somePackPurchase() {
    return CreditPurchase.builder()
        .id("purchase_id")
        .userId("buyer_id")
        .type(PACK)
        .creditPack(CreditPack.builder().id("pack_id").description("Pack 30 crédits").build())
        .quantity(1)
        .credits(30L)
        .creditUnitPriceInCentsWithoutVat(100L)
        .amountInCentsWithoutVat(3000L)
        .amountInCentsWithVat(3600L)
        .vatPercent(2000L)
        .status(COMPLETED)
        .completionDatetime(COMPLETED_AT);
  }

  private CreditOperationInvoiceRequested somePurchaseEvent(long credits) {
    return CreditOperationInvoiceRequested.builder()
        .creditTransaction(somePurchaseTransaction(credits).build())
        .build();
  }

  private CreditTransaction.CreditTransactionBuilder somePurchaseTransaction(long credits) {
    return CreditTransaction.builder()
        .id("transaction_id")
        .userId("buyer_id")
        .type(PURCHASE)
        .movementType(CREDIT)
        .credits(credits)
        .creditPurchaseId("purchase_id");
  }
}
