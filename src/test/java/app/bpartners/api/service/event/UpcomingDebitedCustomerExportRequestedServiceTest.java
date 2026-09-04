package app.bpartners.api.service.event;

import static app.bpartners.api.endpoint.rest.model.CustomerStatus.ENABLED;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.CustomerExportHistorySaved;
import app.bpartners.api.endpoint.event.model.UpcomingDebitedCustomerExportRequested;
import app.bpartners.api.file.bucket.BucketComponent;
import app.bpartners.api.model.*;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.jpa.CustomerExportHistoryJpaRepository;
import app.bpartners.api.service.customer.CustomerExportPayload;
import app.bpartners.api.service.customer.CustomerService;
import app.bpartners.api.service.file.CustomerExportFunction;
import app.bpartners.api.service.subscription.StripeCustomerService;
import app.bpartners.api.service.subscription.UpcomingDebitedCustomers;
import app.bpartners.api.service.subscription.UpcomingUserDebitService;
import app.bpartners.api.service.user.UserService;
import java.io.File;
import java.time.Instant;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UpcomingDebitedCustomerExportRequestedServiceTest {

  UserService userServiceMock = mock(UserService.class);
  CustomerService customerServiceMock = mock(CustomerService.class);
  UpcomingUserDebitService upcomingUserDebitServiceMock = mock(UpcomingUserDebitService.class);
  UserSubscriptionConf userSubscriptionConfMock = mock(UserSubscriptionConf.class);
  CustomerExportFunction customerExportFunctionMock = mock(CustomerExportFunction.class);
  BucketComponent bucketComponentMock = mock(BucketComponent.class);
  CustomerExportHistoryJpaRepository customerExportHistoryJpaRepositoryMock =
      mock(CustomerExportHistoryJpaRepository.class);
  EventProducer eventProducerMock = mock(EventProducer.class);
  StripeCustomerService stripeCustomerServiceMock = mock(StripeCustomerService.class);

  UpcomingDebitedCustomerExportRequestedService subject =
      new UpcomingDebitedCustomerExportRequestedService(
          userServiceMock,
          customerServiceMock,
          upcomingUserDebitServiceMock,
          userSubscriptionConfMock,
          customerExportFunctionMock,
          bucketComponentMock,
          customerExportHistoryJpaRepositoryMock,
          eventProducerMock,
          stripeCustomerServiceMock);

  @Test
  @SuppressWarnings("unchecked")
  void export_billed_and_not_billed_customers_from_the_trigger_snapshot() {
    var userOwnerIdentifier = randomUUID().toString();
    var userSubscriptionId = randomUUID().toString();
    var mail = "debit@example.com";
    var userMock = mock(User.class);
    var billedUserMock = mock(User.class);
    var customerMock = mock(Customer.class);
    var exportedExcelFileMock = mock(File.class);
    var billedStripeCustomerMock = mock(com.stripe.model.Customer.class);
    var billedStripeCreated = Instant.now().getEpochSecond();

    var notBilledStripeCustomerMock = mock(com.stripe.model.Customer.class);
    var notBilledStripeCustomerId = randomUUID().toString();
    var notBilledStripeCreated = Instant.now().minusSeconds(3600L).getEpochSecond();

    when(userSubscriptionConfMock.getUserToCreditId()).thenReturn(userOwnerIdentifier);
    when(userServiceMock.getUserById(userOwnerIdentifier)).thenReturn(userMock);
    when(userMock.getId()).thenReturn(userOwnerIdentifier);
    when(billedUserMock.getEmail()).thenReturn(mail);
    when(billedUserMock.getUserSubscriptionId()).thenReturn(userSubscriptionId);
    when(upcomingUserDebitServiceMock.getUpcomingDebitedCustomers())
        .thenReturn(
            new UpcomingDebitedCustomers(
                List.of(billedUserMock), List.of(notBilledStripeCustomerMock)));

    when(billedStripeCustomerMock.getName()).thenReturn("customer name");
    when(billedStripeCustomerMock.getCreated()).thenReturn(billedStripeCreated);
    when(stripeCustomerServiceMock.getCustomer(billedUserMock))
        .thenReturn(billedStripeCustomerMock);
    when(customerMock.getName()).thenReturn("customer name");
    when(customerMock.getEmail()).thenReturn(mail);
    when(notBilledStripeCustomerMock.getId()).thenReturn(notBilledStripeCustomerId);
    when(notBilledStripeCustomerMock.getName()).thenReturn("unknown customer name");
    when(notBilledStripeCustomerMock.getEmail()).thenReturn("unknown@example.com");
    when(notBilledStripeCustomerMock.getCreated()).thenReturn(notBilledStripeCreated);
    when(customerServiceMock.getCustomers(
            eq(userOwnerIdentifier),
            any(),
            any(),
            eq(mail),
            any(),
            any(),
            any(),
            anyList(),
            any(),
            eq(ENABLED),
            any(),
            any()))
        .thenReturn(List.of(customerMock));
    when(customerExportFunctionMock.apply(any())).thenReturn(exportedExcelFileMock);
    when(bucketComponentMock.upload(eq(exportedExcelFileMock), anyString(), eq(true)))
        .thenReturn(mock());
    when(customerExportHistoryJpaRepositoryMock.save(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    assertDoesNotThrow(
        () -> subject.accept(new UpcomingDebitedCustomerExportRequested(YearMonth.of(2026, 5))));

    var payloadsCaptor = ArgumentCaptor.forClass(List.class);
    verify(customerExportFunctionMock).apply(payloadsCaptor.capture());
    List<CustomerExportPayload> payloads = payloadsCaptor.getValue();
    assertEquals(2, payloads.size());

    var billedPayload =
        payloads.stream().filter(payload -> !payload.unknown()).findFirst().orElseThrow();
    assertEquals("customer name", billedPayload.internalCustomerName());
    assertEquals(mail, billedPayload.email());
    assertEquals(userSubscriptionId, billedPayload.stripeCustomerId());
    assertEquals("customer name", billedPayload.stripeCustomerName());
    assertFalse(billedPayload.unknown());
    assertEquals(
        Instant.ofEpochSecond(billedStripeCreated), billedPayload.stripeCreationDatetime());

    var notBilledPayload =
        payloads.stream().filter(CustomerExportPayload::unknown).findFirst().orElseThrow();
    assertEquals(null, notBilledPayload.internalCustomerName());
    assertEquals("unknown@example.com", notBilledPayload.email());
    assertEquals(notBilledStripeCustomerId, notBilledPayload.stripeCustomerId());
    assertEquals("unknown customer name", notBilledPayload.stripeCustomerName());
    assertTrue(notBilledPayload.unknown());
    assertEquals(
        Instant.ofEpochSecond(notBilledStripeCreated), notBilledPayload.stripeCreationDatetime());

    var additionalProperties = new HashMap<String, Object>();
    additionalProperties.put("month", 5);
    additionalProperties.put("year", 2026);
    additionalProperties.put("usage", "Invoice tracking");

    var customerExportHistoryCaptor = ArgumentCaptor.forClass(CustomerExportHistory.class);
    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(customerExportHistoryJpaRepositoryMock).save(customerExportHistoryCaptor.capture());
    verify(eventProducerMock).accept(listCaptor.capture());
    var actualSavedCustomerExportHistory = customerExportHistoryCaptor.getValue();
    var customerExportHistorySaved = (CustomerExportHistorySaved) listCaptor.getValue().getFirst();
    assertEquals(
        CustomerExportHistory.builder()
            .id(actualSavedCustomerExportHistory.getId())
            .userOwnerIdentifier(userOwnerIdentifier)
            .fileKey(actualSavedCustomerExportHistory.getFileKey())
            .additionalProperties(additionalProperties)
            .creationDatetime(actualSavedCustomerExportHistory.getCreationDatetime())
            .build(),
        actualSavedCustomerExportHistory);
    assertEquals(
        new CustomerExportHistorySaved(actualSavedCustomerExportHistory.getId()),
        customerExportHistorySaved);
  }

  @Test
  void export_history_is_labelled_with_the_billed_month_carried_by_the_event() {
    var userOwnerIdentifier = randomUUID().toString();
    when(userSubscriptionConfMock.getUserToCreditId()).thenReturn(userOwnerIdentifier);
    when(userServiceMock.getUserById(userOwnerIdentifier)).thenReturn(mock(User.class));
    when(upcomingUserDebitServiceMock.getUpcomingDebitedCustomers())
        .thenReturn(new UpcomingDebitedCustomers(List.of(), List.of()));
    when(customerExportFunctionMock.apply(any())).thenReturn(mock(File.class));
    when(bucketComponentMock.upload(any(), anyString(), eq(true))).thenReturn(mock());
    when(customerExportHistoryJpaRepositoryMock.save(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var billedMonth = YearMonth.now().minusMonths(6);
    subject.accept(new UpcomingDebitedCustomerExportRequested(billedMonth));

    var historyCaptor = ArgumentCaptor.forClass(CustomerExportHistory.class);
    verify(customerExportHistoryJpaRepositoryMock).save(historyCaptor.capture());
    var additionalProperties = historyCaptor.getValue().getAdditionalProperties();
    assertEquals(billedMonth.getMonthValue(), additionalProperties.get("month"));
    assertEquals(billedMonth.getYear(), additionalProperties.get("year"));
  }
}
