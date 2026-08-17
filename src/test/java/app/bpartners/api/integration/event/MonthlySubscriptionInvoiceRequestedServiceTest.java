package app.bpartners.api.integration.event;

import static app.bpartners.api.model.subscription.Subscription.SubscriptionStatus.ACTIVE;
import static app.bpartners.api.model.subscription.SubscriptionConsumptionType.ROOF_ANALYSIS;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.model.MonthlySubscriptionInvoiceRequested;
import app.bpartners.api.endpoint.rest.model.*;
import app.bpartners.api.model.*;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceDiscount;
import app.bpartners.api.model.User;
import app.bpartners.api.model.subscription.*;
import app.bpartners.api.model.subscription.UserSubscription;
import app.bpartners.api.payment.StripeConf;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.CustomerRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import app.bpartners.api.repository.jpa.UserStripeCustomerEmailCorrespondenceJpaRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionEligibleJpaRepository;
import app.bpartners.api.service.customer.UserCustomerConverter;
import app.bpartners.api.service.event.MonthlySubscriptionInvoiceRequestedService;
import app.bpartners.api.service.invoice.InvoiceService;
import app.bpartners.api.service.subscription.StripeFactory;
import app.bpartners.api.service.subscription.StripeInvoiceService;
import app.bpartners.api.service.subscription.SubscriptionInvoiceTitleComputer;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import app.bpartners.api.service.utils.TemporalUtils;
import com.stripe.exception.StripeException;
import com.stripe.model.Plan;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.SubscriptionItemCollection;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MonthlySubscriptionInvoiceRequestedServiceTest {
  InvoiceService invoiceServiceMock = mock();
  UserRepository userRepositoryMock = mock();
  CustomerRepository customerRepositoryMock = mock();
  SubscriptionService subscriptionServiceMock = mock();
  UserSubscriptionConf userSubscriptionConfMock = mock();
  UserSubscriptionEligibleJpaRepository subscriptionEligibleJpaRepositoryMock = mock();
  CustomDateFormatter customDateFormatter = new CustomDateFormatter();
  TemporalUtils temporalUtils = new TemporalUtils();
  StripeConf stripeConfMock = mock();
  UserCustomerConverter userCustomerConverter =
      new UserCustomerConverter(userSubscriptionConfMock, customerRepositoryMock);
  StripeFactory stripeFactoryMock = mock();
  StripeInvoiceService stripeInvoiceServiceMock = mock();
  UserStripeCustomerEmailCorrespondenceJpaRepository
      userStripeCustomerEmailCorrespondenceJpaRepositoryMock = mock();
  SubscriptionProductRepository subscriptionProductRepositoryMock = mock();
  MonthlySubscriptionInvoiceRequestedService subject =
      new MonthlySubscriptionInvoiceRequestedService(
          invoiceServiceMock,
          customerRepositoryMock,
          subscriptionServiceMock,
          customDateFormatter,
          temporalUtils,
          subscriptionEligibleJpaRepositoryMock,
          userCustomerConverter,
          stripeConfMock,
          stripeFactoryMock,
          stripeInvoiceServiceMock,
          userStripeCustomerEmailCorrespondenceJpaRepositoryMock,
          new SubscriptionInvoiceTitleComputer(customDateFormatter),
          subscriptionProductRepositoryMock);

  @BeforeEach
  void setUp() {
    var stripeInvoiceMock = mock(com.stripe.model.Invoice.class);
    when(stripeInvoiceMock.getNextPaymentAttempt())
        .thenReturn(temporalUtils.getFirstOfMonthAt2359(now(), 0).minus(1L, DAYS).getEpochSecond());
    when(stripeInvoiceServiceMock.getUpcomingStripeInvoice(any())).thenReturn(stripeInvoiceMock);
    when(userStripeCustomerEmailCorrespondenceJpaRepositoryMock.findByUserId(any()))
        .thenReturn(Optional.empty());
    when(subscriptionProductRepositoryMock.findByE2Id("essentialProduct"))
        .thenReturn(
            Optional.of(
                SubscriptionProduct.builder()
                    .billingType(
                        app.bpartners.api.model.subscription.SubscriptionBillingType.COMMITMENT)
                    .priceInCentsWithoutVat(4900L)
                    .vatPercent(2000L)
                    .freeUsageThreshold(20L)
                    .overageUnitPriceInCents(200L)
                    .build()));
  }

  @Test
  void test_invoice_product_for_extra_analysis() throws StripeException {
    var userPage = 1;
    var userToCredit = User.builder().build();
    var userToCreditId = "userToCreditId";
    var userSubscriptionId = "subscriptionId";
    var subscriptionEligibilityMock = mock(UserSubscriptionEligible.class);
    var userToDebitId = randomUUID().toString();
    when(userSubscriptionConfMock.getUserToCreditId()).thenReturn(userToCreditId);
    when(userRepositoryMock.getById(anyString())).thenReturn(userToCredit);
    when(subscriptionEligibilityMock.getTrialPeriodDays()).thenReturn(0);
    when(subscriptionEligibilityMock.getEligibleFrom()).thenReturn(LocalDate.of(2025, 3, 11));
    when(subscriptionEligibilityMock.hasFreeTrialPeriodActive()).thenReturn(false);
    var accountHolder = AccountHolder.builder().build();
    var subscribedUser =
        User.builder()
            .id(userToDebitId)
            .userSubscriptionId(userSubscriptionId)
            .accountHolders(List.of(accountHolder))
            .build();
    var users = List.of(subscribedUser);
    when(userRepositoryMock.findAllByCriteria(any())).thenReturn(users);
    var subscriptionProduct = SubscriptionProduct.builder().priceInCentsWithoutVat(5L).build();

    var latestSubscription =
        Subscription.builder()
            .subscriptionProduct(subscriptionProduct)
            .active(true)
            .status(ACTIVE)
            .endDatetime(new TemporalUtils().getFirstOfMonthAt2359(now(), 1).minus(1L, DAYS))
            .build();
    var user = User.builder().id(userToDebitId).userSubscriptionId("subscriptionId").build();
    when(stripeConfMock.getBasicSubscriptionProductId()).thenReturn("basicProductId");
    var subscription = mock(com.stripe.model.Subscription.class);
    when(stripeFactoryMock.retrieveUserSubscriptions(any())).thenReturn(List.of(subscription));
    var items = mock(SubscriptionItemCollection.class);
    when(subscription.getItems()).thenReturn(items);
    var data = mock(SubscriptionItem.class);
    when(items.getData()).thenReturn(List.of(data));
    var plan = mock(Plan.class);
    when(data.getPlan()).thenReturn(plan);
    when(plan.getProduct()).thenReturn("essentialProduct");
    var userSubscription =
        UserSubscription.builder().user(user).subscriptions(List.of(latestSubscription)).build();
    when(subscriptionServiceMock.getSubscriptionByUser(any())).thenReturn(userSubscription);
    var customerToDebit = Customer.builder().name("dummy").build();
    when(customerRepositoryMock.findByIdUserAndCriteria(
            any(), any(), any(), any(), any(), any(), any(), anyList(), any(), any(), anyInt(),
            anyInt()))
        .thenReturn(List.of(customerToDebit));
    var variableConsumptionUsage = 24L;
    var consumptionUsageSummary =
        new ConsumptionUsageSummary(ROOF_ANALYSIS, variableConsumptionUsage);
    when(subscriptionServiceMock.computeMonthlySubscriptionVariableConsumption(any()))
        .thenReturn(
            List.of(
                consumptionUsageSummary,
                consumptionUsageSummary,
                consumptionUsageSummary,
                consumptionUsageSummary));
    var invoice = Invoice.builder().customer(customerToDebit).build();
    when(invoiceServiceMock.crupdateSubscriptionInvoice(any())).thenReturn(invoice);
    when(subscriptionEligibleJpaRepositoryMock.findByUserId(userToDebitId))
        .thenReturn(Optional.of(subscriptionEligibilityMock));

    assertDoesNotThrow(
        () ->
            subject.accept(
                MonthlySubscriptionInvoiceRequested.builder()
                    .userToCredit(userToCredit)
                    .userToAttemptDebit(subscribedUser)
                    .build()));

    var invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
    verify(invoiceServiceMock).crupdateSubscriptionInvoice(invoiceCaptor.capture());
    var invoiceCaptorValue = invoiceCaptor.getValue();
    assertEquals(2, invoiceCaptorValue.getProducts().size());
    assertEquals(
        "Analyse de toîtures supplémentaire",
        invoiceCaptorValue.getProducts().get(1).getDescription());
  }

  @Test
  void invoice_amounts_follow_the_resolved_plan_pricing() throws StripeException {
    var userToCredit = User.builder().build();
    var userToCreditId = "userToCreditId";
    var userToDebitId = randomUUID().toString();
    var subscriptionEligibilityMock = mock(UserSubscriptionEligible.class);
    when(userSubscriptionConfMock.getUserToCreditId()).thenReturn(userToCreditId);
    when(userRepositoryMock.getById(anyString())).thenReturn(userToCredit);
    when(subscriptionEligibilityMock.hasFreeTrialPeriodActive()).thenReturn(false);
    when(subscriptionEligibleJpaRepositoryMock.findByUserId(userToDebitId))
        .thenReturn(Optional.of(subscriptionEligibilityMock));
    var subscribedUser =
        User.builder()
            .id(userToDebitId)
            .userSubscriptionId("subscriptionId")
            .accountHolders(List.of(AccountHolder.builder().build()))
            .build();
    var latestSubscription =
        Subscription.builder()
            .active(true)
            .status(ACTIVE)
            .endDatetime(new TemporalUtils().getFirstOfMonthAt2359(now(), 1).minus(1L, DAYS))
            .build();
    var userSubscription =
        UserSubscription.builder()
            .user(subscribedUser)
            .subscriptions(List.of(latestSubscription))
            .build();
    when(subscriptionServiceMock.getSubscriptionByUser(any())).thenReturn(userSubscription);
    var subscription = mock(com.stripe.model.Subscription.class);
    when(stripeFactoryMock.retrieveUserSubscriptions(any())).thenReturn(List.of(subscription));
    var items = mock(SubscriptionItemCollection.class);
    when(subscription.getItems()).thenReturn(items);
    var data = mock(SubscriptionItem.class);
    when(items.getData()).thenReturn(List.of(data));
    var plan = mock(Plan.class);
    when(data.getPlan()).thenReturn(plan);
    when(plan.getProduct()).thenReturn("planBProduct");
    when(subscriptionProductRepositoryMock.findByE2Id("planBProduct"))
        .thenReturn(
            Optional.of(
                SubscriptionProduct.builder()
                    .billingType(
                        app.bpartners.api.model.subscription.SubscriptionBillingType.COMMITMENT)
                    .priceInCentsWithoutVat(700L)
                    .vatPercent(2000L)
                    .freeUsageThreshold(5L)
                    .overageUnitPriceInCents(300L)
                    .build()));
    when(customerRepositoryMock.findByIdUserAndCriteria(
            any(), any(), any(), any(), any(), any(), any(), anyList(), any(), any(), anyInt(),
            anyInt()))
        .thenReturn(List.of(Customer.builder().name("dummy").build()));
    when(subscriptionServiceMock.computeMonthlySubscriptionVariableConsumption(any()))
        .thenReturn(List.of(new ConsumptionUsageSummary(ROOF_ANALYSIS, 8L)));
    when(invoiceServiceMock.crupdateSubscriptionInvoice(any()))
        .thenReturn(Invoice.builder().customer(Customer.builder().name("dummy").build()).build());

    subject.accept(
        MonthlySubscriptionInvoiceRequested.builder()
            .userToCredit(userToCredit)
            .userToAttemptDebit(subscribedUser)
            .build());

    var invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
    verify(invoiceServiceMock).crupdateSubscriptionInvoice(invoiceCaptor.capture());
    var products = invoiceCaptor.getValue().getProducts();
    assertEquals(2, products.size());
    assertEquals(parseFraction(700), products.getFirst().getUnitPrice());
    assertEquals(3, products.get(1).getQuantity());
    assertEquals(parseFraction(300), products.get(1).getUnitPrice());
  }

  @Test
  void does_not_recreate_invoice_when_already_computed_even_if_amount_differs()
      throws StripeException {
    var userToCreditId = "userToCreditId";
    var userToDebitId = "userToDebitId";
    var customerName = "customerName";
    var userToCreditMock = mock(User.class);
    var userToDebitMock = mock(User.class);
    var resolvedCustomerMock = mock(Customer.class);
    var userSubscriptionMock = mock(UserSubscription.class);
    var subscriptionMock = mock(Subscription.class);
    var subscriptionProductMock = mock(SubscriptionProduct.class);
    var subscriptionEligibilityMock = mock(UserSubscriptionEligible.class);
    var userSubscriptionId = "notNullSubscription";

    when(stripeConfMock.getBasicSubscriptionProductId()).thenReturn("basicProductId");
    var subscription = mock(com.stripe.model.Subscription.class);
    when(stripeFactoryMock.retrieveUserSubscriptions(any())).thenReturn(List.of(subscription));
    var items = mock(SubscriptionItemCollection.class);
    when(subscription.getItems()).thenReturn(items);
    var data = mock(SubscriptionItem.class);
    when(items.getData()).thenReturn(List.of(data));
    var plan = mock(Plan.class);
    when(data.getPlan()).thenReturn(plan);
    when(plan.getProduct()).thenReturn("essentialProduct");

    when(userToCreditMock.getId()).thenReturn(userToCreditId);
    when(userToDebitMock.getId()).thenReturn(userToDebitId);
    when(userToDebitMock.getName()).thenReturn("userToDebitName");
    when(userToDebitMock.getEmail()).thenReturn("dummyEmail");
    when(userToDebitMock.getUserSubscriptionId()).thenReturn(userSubscriptionId);
    when(subscriptionEligibilityMock.getTrialPeriodDays()).thenReturn(0);
    when(subscriptionEligibilityMock.getEligibleFrom()).thenReturn(LocalDate.of(2025, 3, 11));
    when(subscriptionProductMock.getName()).thenReturn("subscriptionProductName");
    when(subscriptionProductMock.getVatPercent()).thenReturn(2000L);
    when(subscriptionMock.getSubscriptionProduct()).thenReturn(subscriptionProductMock);
    when(userSubscriptionMock.getLatestSubscription()).thenReturn(subscriptionMock);
    when(userSubscriptionMock.hasValidSubscription()).thenReturn(true);
    when(subscriptionServiceMock.getSubscriptionByUser(userToDebitMock))
        .thenReturn(userSubscriptionMock);
    when(subscriptionEligibleJpaRepositoryMock.findByUserId(userToDebitId))
        .thenReturn(Optional.of(subscriptionEligibilityMock));
    when(resolvedCustomerMock.getName()).thenReturn(customerName);
    when(customerRepositoryMock.findByIdUserAndCriteria(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(Integer.class),
            any(Integer.class)))
        .thenReturn(List.of(resolvedCustomerMock));

    var billingPeriod =
        "pour la période de "
            + customDateFormatter.formatFrenchDate(temporalUtils.startOfLastMonth())
            + " au "
            + customDateFormatter.formatFrenchDate(temporalUtils.endOfLastMonth());
    var alreadyComputedInvoice =
        Invoice.builder()
            .customer(Customer.builder().name(customerName).build())
            .title("Facture " + billingPeriod)
            .createdAt(now())
            // amount intentionally different from the freshly recomputed one: a retry recomputes
            // the
            // variable consumption live, so the price may differ for the same customer and period.
            .totalPriceWithVat(new Fraction(BigInteger.valueOf(999_999L)))
            .build();
    when(invoiceServiceMock.getInvoices(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(List.of(alreadyComputedInvoice));

    assertDoesNotThrow(
        () ->
            subject.accept(
                MonthlySubscriptionInvoiceRequested.builder()
                    .userToCredit(userToCreditMock)
                    .userToAttemptDebit(userToDebitMock)
                    .build()));

    verify(invoiceServiceMock, never()).crupdateSubscriptionInvoice(any());
  }

  @Test
  void generate_invoice_for_paginated_users_with_existing_customers() throws StripeException {
    var userPage = 1;
    var userToCreditId = "userToCreditId";
    var userToDebitId = "userToDebitId";
    var userToCreditMock = mock(User.class);
    var userToDebitMock = mock(User.class);
    var customerMock = mock(Customer.class);
    var customerEmail = "dummyEmail";
    var userSubscriptionMock = mock(UserSubscription.class);
    var subscriptionMock = mock(Subscription.class);
    var subscriptionProductMock = mock(SubscriptionProduct.class);
    var subscriptionEligibilityMock = mock(UserSubscriptionEligible.class);
    var subscriptionProductName = "subscriptionProductName";
    var userSubscriptionId = "notNullSubscription";
    when(stripeConfMock.getBasicSubscriptionProductId()).thenReturn("basicProductId");
    var subscription = mock(com.stripe.model.Subscription.class);
    when(stripeFactoryMock.retrieveUserSubscriptions(any())).thenReturn(List.of(subscription));
    var items = mock(SubscriptionItemCollection.class);
    when(subscription.getItems()).thenReturn(items);
    var data = mock(SubscriptionItem.class);
    when(items.getData()).thenReturn(List.of(data));
    var plan = mock(Plan.class);
    when(data.getPlan()).thenReturn(plan);
    when(plan.getProduct()).thenReturn("essentialProduct");

    when(userSubscriptionConfMock.getUserToCreditId()).thenReturn(userToCreditId);
    when(userRepositoryMock.getById(userToCreditId)).thenReturn(userToCreditMock);
    when(userRepositoryMock.findAllByCriteria(any())).thenReturn(List.of(userToDebitMock));
    when(invoiceServiceMock.crupdateSubscriptionInvoice(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(userToCreditMock.getId()).thenReturn(userToCreditId);
    when(userToDebitMock.getId()).thenReturn(userToDebitId);
    when(userToDebitMock.getName()).thenReturn("userToDebitName");
    when(userToDebitMock.getEmail()).thenReturn(customerEmail);
    when(userToDebitMock.getUserSubscriptionId()).thenReturn(userSubscriptionId);
    when(userToDebitMock.getUserSubscriptionId()).thenReturn(userSubscriptionId);
    when(subscriptionEligibilityMock.getTrialPeriodDays()).thenReturn(0);
    when(subscriptionEligibilityMock.getEligibleFrom()).thenReturn(LocalDate.of(2025, 3, 11));
    when(subscriptionProductMock.getName()).thenReturn(subscriptionProductName);
    when(subscriptionProductMock.getVatPercent()).thenReturn(2000L);
    when(subscriptionMock.getSubscriptionProduct()).thenReturn(subscriptionProductMock);
    when(userSubscriptionMock.getLatestSubscription()).thenReturn(subscriptionMock);
    when(subscriptionMock.getEndDatetime())
        .thenReturn(new TemporalUtils().getFirstOfMonthAt2359(now(), 1).minus(1L, DAYS));
    when(userSubscriptionMock.hasValidSubscription()).thenReturn(true);
    when(customerRepositoryMock.findByIdUserAndCriteria(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(Integer.class),
            any(Integer.class)))
        .thenReturn(List.of(customerMock));
    when(subscriptionServiceMock.getSubscriptionByUser(userToDebitMock))
        .thenReturn(userSubscriptionMock);
    when(subscriptionEligibleJpaRepositoryMock.findByUserId(userToDebitId))
        .thenReturn(Optional.of(subscriptionEligibilityMock));

    assertDoesNotThrow(
        () ->
            subject.accept(
                MonthlySubscriptionInvoiceRequested.builder()
                    .userToCredit(userToCreditMock)
                    .userToAttemptDebit(userToDebitMock)
                    .build()));

    // var eventCaptor = ArgumentCaptor.forClass(List.class);
    var invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
    // verify(eventProducerMock).accept(eventCaptor.capture());
    verify(invoiceServiceMock).crupdateSubscriptionInvoice(invoiceCaptor.capture());
    verify(customerRepositoryMock, never()).save(any());
    // var monthlySubscriptionInvoiceCreated =
    //   (MonthlySubscriptionInvoiceCreated) eventCaptor.getValue().getFirst();
    var createdInvoice = invoiceCaptor.getValue();
    var actualInvoiceProduct = createdInvoice.getProducts().getFirst();
    var expectedInvoice = computeExpectedInvoice(createdInvoice, userToCreditMock, customerMock);
    var expectedInvoiceProduct =
        computeExpectedInvoiceProduct(
            actualInvoiceProduct, expectedInvoice, subscriptionProductName);

    assertEquals(expectedInvoice, createdInvoice);
    // assertEquals(monthlySubscriptionInvoiceCreated.getInvoiceId(), createdInvoice.getId());
    assertEquals(1, createdInvoice.getProducts().size());
    assertEquals(expectedInvoiceProduct, actualInvoiceProduct);
    // assertEquals(Duration.ofSeconds(300L),
    // monthlySubscriptionInvoiceCreated.maxConsumerDuration());
    // assertEquals(
    //  Duration.ofSeconds(60L),
    // monthlySubscriptionInvoiceCreated.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void generate_invoice_for_user_with_stripe_customer_email_correspondence()
      throws StripeException {
    var userToCreditId = "userToCreditId";
    var userToDebitId = "userToDebitId";
    var userToCreditMock = mock(User.class);
    var userToDebitMock = mock(User.class);
    var customerMock = mock(Customer.class);
    var userOriginalEmail = "userOriginalEmail";
    var correspondenceEmail = "correspondenceEmail";
    var userSubscriptionMock = mock(UserSubscription.class);
    var subscriptionMock = mock(Subscription.class);
    var subscriptionProductMock = mock(SubscriptionProduct.class);
    var subscriptionEligibilityMock = mock(UserSubscriptionEligible.class);
    var subscriptionProductName = "subscriptionProductName";
    var userSubscriptionId = "notNullSubscription";
    when(stripeConfMock.getBasicSubscriptionProductId()).thenReturn("basicProductId");
    var subscription = mock(com.stripe.model.Subscription.class);
    when(stripeFactoryMock.retrieveUserSubscriptions(any())).thenReturn(List.of(subscription));
    var items = mock(SubscriptionItemCollection.class);
    when(subscription.getItems()).thenReturn(items);
    var data = mock(SubscriptionItem.class);
    when(items.getData()).thenReturn(List.of(data));
    var plan = mock(Plan.class);
    when(data.getPlan()).thenReturn(plan);
    when(plan.getProduct()).thenReturn("essentialProduct");

    when(userSubscriptionConfMock.getUserToCreditId()).thenReturn(userToCreditId);
    when(userRepositoryMock.getById(userToCreditId)).thenReturn(userToCreditMock);
    when(userRepositoryMock.findAllByCriteria(any())).thenReturn(List.of(userToDebitMock));
    when(invoiceServiceMock.crupdateSubscriptionInvoice(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(userToCreditMock.getId()).thenReturn(userToCreditId);
    when(userToDebitMock.getId()).thenReturn(userToDebitId);
    when(userToDebitMock.getName()).thenReturn("userToDebitName");
    when(userToDebitMock.getEmail()).thenReturn(userOriginalEmail);
    when(userToDebitMock.getUserSubscriptionId()).thenReturn(userSubscriptionId);
    when(subscriptionEligibilityMock.getTrialPeriodDays()).thenReturn(0);
    when(subscriptionEligibilityMock.getEligibleFrom()).thenReturn(LocalDate.of(2025, 3, 11));
    when(subscriptionProductMock.getName()).thenReturn(subscriptionProductName);
    when(subscriptionProductMock.getVatPercent()).thenReturn(2000L);
    when(subscriptionMock.getSubscriptionProduct()).thenReturn(subscriptionProductMock);
    when(userSubscriptionMock.getLatestSubscription()).thenReturn(subscriptionMock);
    when(subscriptionMock.getEndDatetime())
        .thenReturn(new TemporalUtils().getFirstOfMonthAt2359(now(), 1).minus(1L, DAYS));
    when(userSubscriptionMock.hasValidSubscription()).thenReturn(true);
    when(customerRepositoryMock.findByIdUserAndCriteria(
            any(),
            any(),
            any(),
            eq(userOriginalEmail),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(Integer.class),
            any(Integer.class)))
        .thenReturn(List.of());
    var correspondence =
        UserStripeCustomerEmailCorrespondence.builder()
            .id(randomUUID().toString())
            .userId(userToDebitId)
            .stripeCustomerId("stripeCustomerId")
            .email(correspondenceEmail)
            .build();
    when(userStripeCustomerEmailCorrespondenceJpaRepositoryMock.findByUserId(userToDebitId))
        .thenReturn(Optional.of(correspondence));
    when(customerRepositoryMock.findByIdUserAndCriteria(
            any(),
            any(),
            any(),
            eq(correspondenceEmail),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(Integer.class),
            any(Integer.class)))
        .thenReturn(List.of(customerMock));
    when(subscriptionServiceMock.getSubscriptionByUser(userToDebitMock))
        .thenReturn(userSubscriptionMock);
    when(subscriptionEligibleJpaRepositoryMock.findByUserId(userToDebitId))
        .thenReturn(Optional.of(subscriptionEligibilityMock));

    assertDoesNotThrow(
        () ->
            subject.accept(
                MonthlySubscriptionInvoiceRequested.builder()
                    .userToCredit(userToCreditMock)
                    .userToAttemptDebit(userToDebitMock)
                    .build()));

    var invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
    verify(invoiceServiceMock).crupdateSubscriptionInvoice(invoiceCaptor.capture());
    verify(userStripeCustomerEmailCorrespondenceJpaRepositoryMock).findByUserId(userToDebitId);
    verify(customerRepositoryMock, never()).save(any());
    var createdInvoice = invoiceCaptor.getValue();
    var expectedInvoice = computeExpectedInvoice(createdInvoice, userToCreditMock, customerMock);
    var expectedInvoiceProduct =
        computeExpectedInvoiceProduct(
            createdInvoice.getProducts().getFirst(), expectedInvoice, subscriptionProductName);

    assertEquals(customerMock, createdInvoice.getCustomer());
    assertEquals(expectedInvoice, createdInvoice);
    assertEquals(1, createdInvoice.getProducts().size());
    assertEquals(expectedInvoiceProduct, createdInvoice.getProducts().getFirst());
  }

  @Test
  void generate_invoice_falls_back_to_converter_when_correspondence_customer_missing()
      throws StripeException {
    var userToCreditId = "userToCreditId";
    var userToDebitId = randomUUID().toString();
    var userToCreditMock = mock(User.class);
    var userToDebitMock = mock(User.class);
    var holderMock = accountHolderWithValuesMock(mock(AccountHolder.class));
    var userOriginalEmail = "dummyEmail";
    var correspondenceEmail = "unknownCorrespondenceEmail";
    var customerFirstName = "customerFirstName";
    var customerLastName = "customerLastName";
    var userSubscriptionMock = mock(UserSubscription.class);
    var subscriptionMock = mock(Subscription.class);
    var subscriptionProductMock = mock(SubscriptionProduct.class);
    var subscriptionEligibilityMock = mock(UserSubscriptionEligible.class);
    var subscriptionProductName = "subscriptionProductName";
    var userSubscriptionId = "notNullSubscription";
    var adminUserId = randomUUID().toString();
    var adminUserMock = mock(User.class);
    when(stripeConfMock.getBasicSubscriptionProductId()).thenReturn("basicProductId");
    var subscription = mock(com.stripe.model.Subscription.class);
    when(stripeFactoryMock.retrieveUserSubscriptions(any())).thenReturn(List.of(subscription));
    var items = mock(SubscriptionItemCollection.class);
    when(subscription.getItems()).thenReturn(items);
    var data = mock(SubscriptionItem.class);
    when(items.getData()).thenReturn(List.of(data));
    var plan = mock(Plan.class);
    when(data.getPlan()).thenReturn(plan);
    when(plan.getProduct()).thenReturn("essentialProduct");

    when(userSubscriptionConfMock.getUserToCreditId()).thenReturn(userToCreditId);
    when(userRepositoryMock.getById(userToCreditId)).thenReturn(userToCreditMock);
    when(userRepositoryMock.findAllByCriteria(any())).thenReturn(List.of(userToDebitMock));
    when(invoiceServiceMock.crupdateSubscriptionInvoice(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(userToCreditMock.getId()).thenReturn(userToCreditId);
    when(userToDebitMock.getId()).thenReturn(userToDebitId);
    when(userToDebitMock.getName()).thenReturn("userToDebitName");
    when(userToDebitMock.getDefaultHolder()).thenReturn(holderMock);
    when(userToDebitMock.getEmail()).thenReturn(userOriginalEmail);
    when(userToDebitMock.getFirstName()).thenReturn(customerFirstName);
    when(userToDebitMock.getLastName()).thenReturn(customerLastName);
    when(userToDebitMock.getUserSubscriptionId()).thenReturn(userSubscriptionId);
    when(subscriptionEligibilityMock.getTrialPeriodDays()).thenReturn(0);
    when(subscriptionEligibilityMock.getEligibleFrom()).thenReturn(LocalDate.of(2025, 3, 11));
    when(subscriptionProductMock.getName()).thenReturn(subscriptionProductName);
    when(subscriptionProductMock.getVatPercent()).thenReturn(2000L);
    when(subscriptionMock.getSubscriptionProduct()).thenReturn(subscriptionProductMock);
    when(subscriptionMock.getEndDatetime())
        .thenReturn(new TemporalUtils().getFirstOfMonthAt2359(now(), 1).minus(1L, DAYS));
    when(userSubscriptionMock.hasValidSubscription()).thenReturn(true);
    when(userSubscriptionMock.getLatestSubscription()).thenReturn(subscriptionMock);
    when(adminUserMock.getId()).thenReturn(adminUserId);
    when(userRepositoryMock.findByEmail(System.getenv("ADMIN_EMAIL")))
        .thenReturn(Optional.of(adminUserMock));
    when(subscriptionEligibleJpaRepositoryMock.findByUserId(userToDebitMock.getId()))
        .thenReturn(Optional.of(subscriptionEligibilityMock));
    when(customerRepositoryMock.findByIdUserAndCriteria(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(Integer.class),
            any(Integer.class)))
        .thenReturn(List.of());
    var correspondence =
        UserStripeCustomerEmailCorrespondence.builder()
            .id(randomUUID().toString())
            .userId(userToDebitId)
            .stripeCustomerId("stripeCustomerId")
            .email(correspondenceEmail)
            .build();
    when(userStripeCustomerEmailCorrespondenceJpaRepositoryMock.findByUserId(userToDebitId))
        .thenReturn(Optional.of(correspondence));
    when(customerRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(subscriptionServiceMock.getSubscriptionByUser(userToDebitMock))
        .thenReturn(userSubscriptionMock);

    assertDoesNotThrow(
        () ->
            subject.accept(
                MonthlySubscriptionInvoiceRequested.builder()
                    .userToCredit(userToCreditMock)
                    .userToAttemptDebit(userToDebitMock)
                    .build()));

    var invoiceCaptor = forClass(Invoice.class);
    var customerCaptor = forClass(Customer.class);
    verify(invoiceServiceMock).crupdateSubscriptionInvoice(invoiceCaptor.capture());
    verify(userStripeCustomerEmailCorrespondenceJpaRepositoryMock).findByUserId(userToDebitId);
    verify(customerRepositoryMock).save(customerCaptor.capture());
    var createdInvoice = invoiceCaptor.getValue();
    var actualCustomer = customerCaptor.getValue();
    var expectedCreatedCustomer =
        computeExpectedCreatedCustomer(userToCreditId, userToDebitMock, actualCustomer);
    var expectedInvoice =
        computeExpectedInvoice(createdInvoice, userToCreditMock, expectedCreatedCustomer);
    var expectedInvoiceProduct =
        computeExpectedInvoiceProduct(
            createdInvoice.getProducts().getFirst(), expectedInvoice, subscriptionProductName);

    assertEquals(expectedCreatedCustomer, actualCustomer);
    assertEquals(expectedInvoice, createdInvoice);
    assertEquals(1, createdInvoice.getProducts().size());
    assertEquals(expectedInvoiceProduct, createdInvoice.getProducts().getFirst());
  }

  @Test
  void generate_invoice_for_paginated_users_without_existing_customers() {
    var userPage = 1;
    var userToCreditId = "userToCreditId";
    var userToCreditMock = mock(User.class);
    var userToDebitMock = mock(User.class);
    var holderMock = accountHolderWithValuesMock(mock(AccountHolder.class));
    var customerEmail = "dummyEmail";
    var userSubscriptionMock = mock(UserSubscription.class);
    var subscriptionMock = mock(Subscription.class);
    var subscriptionProductMock = mock(SubscriptionProduct.class);
    var subscriptionEligibilityMock = mock(UserSubscriptionEligible.class);
    var subscriptionProductName = "subscriptionProductName";
    var customerFirstName = "customerFirstName";
    var customerLastName = "customerLastName";
    var userSubscriptionId = "notNullSubscription";
    var userToDebitId = randomUUID().toString();
    var adminUserId = randomUUID().toString();
    var adminUserMock = mock(User.class);

    when(userSubscriptionConfMock.getUserToCreditId()).thenReturn(userToCreditId);
    when(userRepositoryMock.getById(userToCreditId)).thenReturn(userToCreditMock);
    when(userRepositoryMock.findAllByCriteria(any())).thenReturn(List.of(userToDebitMock));
    when(invoiceServiceMock.crupdateSubscriptionInvoice(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(userToCreditMock.getId()).thenReturn(userToCreditId);
    when(userToDebitMock.getId()).thenReturn(userToDebitId);
    when(userToDebitMock.getName()).thenReturn("userToDebitName");
    when(userToDebitMock.getDefaultHolder()).thenReturn(holderMock);
    when(userToDebitMock.getEmail()).thenReturn(customerEmail);
    when(userToDebitMock.getFirstName()).thenReturn(customerFirstName);
    when(userToDebitMock.getLastName()).thenReturn(customerLastName);
    when(userToDebitMock.getUserSubscriptionId()).thenReturn(userSubscriptionId);
    when(subscriptionEligibilityMock.getTrialPeriodDays()).thenReturn(0);
    when(subscriptionEligibilityMock.getEligibleFrom()).thenReturn(LocalDate.of(2025, 3, 11));
    when(subscriptionProductMock.getName()).thenReturn(subscriptionProductName);
    when(subscriptionProductMock.getVatPercent()).thenReturn(2000L);
    when(subscriptionMock.getSubscriptionProduct()).thenReturn(subscriptionProductMock);
    when(subscriptionMock.getEndDatetime())
        .thenReturn(new TemporalUtils().getFirstOfMonthAt2359(now(), 1).minus(1L, DAYS));
    when(userSubscriptionMock.hasValidSubscription()).thenReturn(true);
    when(userSubscriptionMock.getLatestSubscription()).thenReturn(subscriptionMock);
    when(adminUserMock.getId()).thenReturn(adminUserId);
    when(userRepositoryMock.findByEmail(System.getenv("ADMIN_EMAIL")))
        .thenReturn(Optional.of(adminUserMock));
    when(subscriptionEligibleJpaRepositoryMock.findByUserId(userToDebitMock.getId()))
        .thenReturn(Optional.of(subscriptionEligibilityMock));
    when(customerRepositoryMock.findByIdUserAndCriteria(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(Integer.class),
            any(Integer.class)))
        .thenReturn(List.of());
    when(customerRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(subscriptionServiceMock.getSubscriptionByUser(userToDebitMock))
        .thenReturn(userSubscriptionMock);

    assertDoesNotThrow(
        () ->
            subject.accept(
                MonthlySubscriptionInvoiceRequested.builder()
                    .userToCredit(userToCreditMock)
                    .userToAttemptDebit(userToDebitMock)
                    .build()));

    // var eventCaptor = forClass(List.class);
    var invoiceCaptor = forClass(Invoice.class);
    var customerCaptor = forClass(Customer.class);
    // verify(eventProducerMock).accept(eventCaptor.capture());
    verify(invoiceServiceMock).crupdateSubscriptionInvoice(invoiceCaptor.capture());
    verify(customerRepositoryMock).save(customerCaptor.capture());
    // var monthlySubscriptionInvoiceCreated =
    // (MonthlySubscriptionInvoiceCreated) eventCaptor.getValue().getFirst();
    var createdInvoice = invoiceCaptor.getValue();
    var actualInvoiceProduct = createdInvoice.getProducts().getFirst();
    var actualCustomer = customerCaptor.getValue();
    var expectedCreatedCustomer =
        computeExpectedCreatedCustomer(userToCreditId, userToDebitMock, actualCustomer);
    var expectedInvoice =
        computeExpectedInvoice(createdInvoice, userToCreditMock, expectedCreatedCustomer);
    var expectedInvoiceProduct =
        computeExpectedInvoiceProduct(
            actualInvoiceProduct, expectedInvoice, subscriptionProductName);

    assertEquals(expectedCreatedCustomer, actualCustomer);
    assertEquals(expectedInvoice, createdInvoice);
    // assertEquals(monthlySubscriptionInvoiceCreated.getInvoiceId(), createdInvoice.getId());
    assertEquals(1, createdInvoice.getProducts().size());
    assertEquals(expectedInvoiceProduct, actualInvoiceProduct);
    // assertEquals(Duration.ofSeconds(300L),
    // monthlySubscriptionInvoiceCreated.maxConsumerDuration());
    // assertEquals(
    //  Duration.ofSeconds(60L),
    // monthlySubscriptionInvoiceCreated.maxConsumerBackoffBetweenRetries());
  }

  private Customer computeExpectedCreatedCustomer(
      String adminUserId, User userToDebit, Customer actual) {
    return Customer.builder()
        .id(actual.getId())
        .idUser(adminUserId)
        .createdAt(actual.getCreatedAt())
        .updatedAt(actual.getUpdatedAt())
        .name("accountHolderToDebitName")
        .firstName(userToDebit.getFirstName())
        .lastName(userToDebit.getLastName())
        .email("dummyEmail")
        .phone("0612345678")
        .website("accountHolderToDebitWebsite")
        .address("accountHolderToDebitAddress")
        .zipCode(75001)
        .city("accountHolderToDebitCity")
        .country("accountHolderToDebitCountry")
        .status(CustomerStatus.ENABLED)
        .customerType(CustomerType.PROFESSIONAL)
        .build();
  }

  private InvoiceProduct computeExpectedInvoiceProduct(
      InvoiceProduct actualInvoiceProduct,
      Invoice expectedInvoice,
      String subscriptionProductName) {
    return InvoiceProduct.builder()
        .id(actualInvoiceProduct.getId())
        .createdAt(actualInvoiceProduct.getCreatedAt())
        .idInvoice(expectedInvoice.getId())
        .description(subscriptionProductName)
        .quantity(1)
        .unitPrice(new Fraction(BigInteger.valueOf(4900L)))
        .vatPercent(new Fraction(BigInteger.valueOf(2000)))
        .vatWithDiscount(new Fraction(BigInteger.valueOf(980)))
        .totalWithDiscount(new Fraction(BigInteger.valueOf(5880)))
        .priceNoVatWithDiscount(new Fraction(BigInteger.valueOf(4900)))
        .status(ProductStatus.ENABLED)
        .build();
  }

  private Invoice computeExpectedInvoice(
      Invoice createdInvoice, User userToCreditMock, Customer customerMock) {
    var startOfBilledMonthFormatted =
        customDateFormatter.formatFrenchDate(temporalUtils.startOfLastMonth());
    var endOfBilledMonthFormatted =
        customDateFormatter.formatFrenchDate(temporalUtils.endOfLastMonth());
    var sendingDate = temporalUtils.endOfLastMonth();
    return Invoice.builder()
        .id(createdInvoice.getId())
        .paymentMethod(PaymentMethod.CREDIT_CARD)
        .subscriptionInvoice(true)
        .discount(InvoiceDiscount.builder().percentValue(new Fraction(BigInteger.ZERO)).build())
        .delayPenaltyPercent(new Fraction(BigInteger.ZERO))
        .paymentType(app.bpartners.api.endpoint.rest.model.Invoice.PaymentTypeEnum.CASH)
        .title(
            "Facture pour la période de "
                + startOfBilledMonthFormatted
                + " au "
                + endOfBilledMonthFormatted)
        .ref(createdInvoice.getRef())
        .validityDate(sendingDate.plusDays(30L))
        .toPayAt(temporalUtils.startOfActualMonth())
        .sendingDate(sendingDate)
        .createdAt(createdInvoice.getCreatedAt())
        .user(userToCreditMock)
        .customer(customerMock)
        .status(InvoiceStatus.CONFIRMED)
        .paymentRegulations(new ArrayList<>())
        .archiveStatus(ArchiveStatus.ENABLED)
        .delayInPaymentAllowed(0)
        .products(createdInvoice.getProducts())
        .totalPriceWithoutDiscount(new Fraction(BigInteger.valueOf(4900)))
        .totalVat(new Fraction(BigInteger.valueOf(980)))
        .totalPriceWithVat(new Fraction(BigInteger.valueOf(5880)))
        .totalPriceWithoutVat(new Fraction(BigInteger.valueOf(4900)))
        .build();
  }

  private AccountHolder accountHolderWithValuesMock(AccountHolder accountHolderMock) {
    when(accountHolderMock.getName()).thenReturn("accountHolderToDebitName");
    when(accountHolderMock.getMobilePhoneNumber()).thenReturn("0612345678");
    when(accountHolderMock.getWebsite()).thenReturn("accountHolderToDebitWebsite");
    when(accountHolderMock.getAddress()).thenReturn("accountHolderToDebitAddress");
    when(accountHolderMock.getPostalCode()).thenReturn("75001");
    when(accountHolderMock.getCity()).thenReturn("accountHolderToDebitCity");
    when(accountHolderMock.getCountry()).thenReturn("accountHolderToDebitCountry");

    return accountHolderMock;
  }
}
