package app.bpartners.api.integration.event;

import static app.bpartners.api.model.subscription.Subscription.SubscriptionStatus.ACTIVE;
import static app.bpartners.api.model.subscription.SubscriptionConsumptionType.ROOF_ANALYSIS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.EventProducer;
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
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.CustomerRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.InvoiceService;
import app.bpartners.api.service.event.MonthlySubscriptionInvoiceRequestedService;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import app.bpartners.api.service.utils.TemporalUtils;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MonthlySubscriptionInvoiceRequestedServiceTest {
  InvoiceService invoiceServiceMock = mock();
  UserRepository userRepositoryMock = mock();
  CustomerRepository customerRepositoryMock = mock();
  SubscriptionService subscriptionServiceMock = mock();
  EventProducer eventProducerMock = mock();
  UserSubscriptionConf userSubscriptionConfMock = mock();
  CustomDateFormatter customDateFormatter = new CustomDateFormatter();
  TemporalUtils temporalUtils = new TemporalUtils();
  MonthlySubscriptionInvoiceRequestedService subject =
      new MonthlySubscriptionInvoiceRequestedService(
          invoiceServiceMock,
          userRepositoryMock,
          customerRepositoryMock,
          subscriptionServiceMock,
          eventProducerMock,
          userSubscriptionConfMock,
          customDateFormatter,
          temporalUtils);

  @Test
  void test_invoice_product_for_extra_analysis() {
    var userPage = 1;
    var userToCredit = User.builder().build();
    var userToCreditId = "userToCreditId";
    when(userSubscriptionConfMock.getUserToCreditId()).thenReturn(userToCreditId);
    when(userRepositoryMock.getById(anyString())).thenReturn(userToCredit);
    var accountHolder = AccountHolder.builder().build();
    var subscribedUser =
        User.builder()
            .userSubscriptionId("subscriptionId")
            .accountHolders(List.of(accountHolder))
            .build();
    var users = List.of(subscribedUser);
    when(userRepositoryMock.findAllByCriteria(any())).thenReturn(users);
    var subscriptionProduct = SubscriptionProduct.builder().priceInCents(5L).build();
    var latestSubscription =
        Subscription.builder().subscriptionProduct(subscriptionProduct).status(ACTIVE).build();
    var userSubscription =
        UserSubscription.builder().subscriptions(List.of(latestSubscription)).build();
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

    assertDoesNotThrow(
        () -> {
          subject.accept(MonthlySubscriptionInvoiceRequested.builder().userPage(userPage).build());
        });

    var invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
    verify(invoiceServiceMock).crupdateSubscriptionInvoice(invoiceCaptor.capture());
    var invoiceCaptorValue = invoiceCaptor.getValue();
    assertEquals(2, invoiceCaptorValue.getProducts().size());
    assertEquals(
        "Analyse de toîtures supplémentaire",
        invoiceCaptorValue.getProducts().get(1).getDescription());
  }

  @Test
  void generate_invoice_for_paginated_users_with_existing_customers() {
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
    var subscriptionProductName = "subscriptionProductName";

    when(userSubscriptionConfMock.getUserToCreditId()).thenReturn(userToCreditId);
    when(userRepositoryMock.getById(userToCreditId)).thenReturn(userToCreditMock);
    when(userRepositoryMock.findAllByCriteria(any())).thenReturn(List.of(userToDebitMock));
    when(invoiceServiceMock.crupdateSubscriptionInvoice(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(userToCreditMock.getId()).thenReturn(userToCreditId);
    when(userToDebitMock.getId()).thenReturn(userToDebitId);
    when(userToDebitMock.getEmail()).thenReturn(customerEmail);
    when(userToDebitMock.getUserSubscriptionId()).thenReturn("notNullSubscription");
    when(subscriptionProductMock.getName()).thenReturn(subscriptionProductName);
    when(subscriptionProductMock.getPriceInCents()).thenReturn(4900L);
    when(subscriptionMock.getSubscriptionProduct()).thenReturn(subscriptionProductMock);
    when(userSubscriptionMock.getLatestSubscription()).thenReturn(subscriptionMock);
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

    assertDoesNotThrow(
        () ->
            subject.accept(
                MonthlySubscriptionInvoiceRequested.builder().userPage(userPage).build()));

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
    var subscriptionProductName = "subscriptionProductName";
    var customerFirstName = "customerFirstName";
    var customerLastName = "customerLastName";

    when(userSubscriptionConfMock.getUserToCreditId()).thenReturn(userToCreditId);
    when(userRepositoryMock.getById(userToCreditId)).thenReturn(userToCreditMock);
    when(userRepositoryMock.findAllByCriteria(any())).thenReturn(List.of(userToDebitMock));
    when(invoiceServiceMock.crupdateSubscriptionInvoice(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(userToCreditMock.getId()).thenReturn(userToCreditId);
    when(userToDebitMock.getDefaultHolder()).thenReturn(holderMock);
    when(userToDebitMock.getEmail()).thenReturn(customerEmail);
    when(userToDebitMock.getFirstName()).thenReturn(customerFirstName);
    when(userToDebitMock.getLastName()).thenReturn(customerLastName);
    when(userToDebitMock.getUserSubscriptionId()).thenReturn("notNullSubscription");
    when(subscriptionProductMock.getName()).thenReturn(subscriptionProductName);
    when(subscriptionProductMock.getPriceInCents()).thenReturn(4900L);
    when(subscriptionMock.getSubscriptionProduct()).thenReturn(subscriptionProductMock);
    when(userSubscriptionMock.getLatestSubscription()).thenReturn(subscriptionMock);
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
                MonthlySubscriptionInvoiceRequested.builder().userPage(userPage).build()));

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
    var expectedCreatedCustomer = computeExpectedCreatedCustomer(userToDebitMock, actualCustomer);
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

  private Customer computeExpectedCreatedCustomer(User userToDebit, Customer actual) {
    return Customer.builder()
        .id(actual.getId())
        .createdAt(actual.getCreatedAt())
        .updatedAt(actual.getUpdatedAt())
        .idUser(userToDebit.getId())
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
    var startOfCurrentMonthFormatted =
        customDateFormatter.formatFrenchDate(temporalUtils.startOfActualMonth());
    var endOfCurrentMonthFormatted =
        customDateFormatter.formatFrenchDate(temporalUtils.endOfActualMonth());
    var sendingDate = LocalDate.of(2025, 1, 31);
    return Invoice.builder()
        .id(createdInvoice.getId())
        .paymentMethod(PaymentMethod.CREDIT_CARD)
        .subscriptionInvoice(true)
        .discount(InvoiceDiscount.builder().percentValue(new Fraction(BigInteger.ZERO)).build())
        .delayPenaltyPercent(new Fraction(BigInteger.ZERO))
        .paymentType(app.bpartners.api.endpoint.rest.model.Invoice.PaymentTypeEnum.CASH)
        .title(
            "Facture pour la période de "
                + startOfCurrentMonthFormatted
                + " au "
                + endOfCurrentMonthFormatted)
        .ref(createdInvoice.getRef())
        .validityDate(sendingDate.plusDays(30L))
        .toPayAt(createdInvoice.getToPayAt())
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
