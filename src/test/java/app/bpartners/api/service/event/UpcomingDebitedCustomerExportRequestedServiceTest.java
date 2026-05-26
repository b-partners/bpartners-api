package app.bpartners.api.service.event;

import static app.bpartners.api.endpoint.rest.model.CustomerStatus.ENABLED;
import static app.bpartners.api.model.BoundedPageSize.MAX_SIZE;
import static app.bpartners.api.model.PageFromOne.MIN_PAGE;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.CustomerExportHistorySaved;
import app.bpartners.api.endpoint.event.model.UpcomingDebitedCustomerExportRequested;
import app.bpartners.api.file.bucket.BucketComponent;
import app.bpartners.api.model.*;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.jpa.CustomerExportHistoryJpaRepository;
import app.bpartners.api.repository.jpa.UnknownStripeCustomerJpaRepository;
import app.bpartners.api.service.customer.CustomerService;
import app.bpartners.api.service.file.CustomerExportFunction;
import app.bpartners.api.service.subscription.StripeCustomerService;
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
  UnknownStripeCustomerJpaRepository unknownStripeCustomerJpaRepositoryMock =
      mock(UnknownStripeCustomerJpaRepository.class);
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
          unknownStripeCustomerJpaRepositoryMock,
          userSubscriptionConfMock,
          customerExportFunctionMock,
          bucketComponentMock,
          customerExportHistoryJpaRepositoryMock,
          eventProducerMock,
          stripeCustomerServiceMock);

  @Test
  void export_upcoming_user_debited_customers() {
    var userOwnerIdentifier = randomUUID().toString();
    var userSubscriptionId = randomUUID().toString();
    var mail = "debit@example.com";
    var userMock = mock(User.class);
    var userDebitedMock = mock(User.class);
    var customerMock = mock(Customer.class);
    var unknownStripeCustomerMock = mock(UnknownStripeCustomer.class);
    var exportedExcelFileMock = mock(File.class);
    var stripeCustomerMock = mock(com.stripe.model.Customer.class);
    var stripeCreated = Instant.now().toEpochMilli() / 1000;

    when(userDebitedMock.getEmail()).thenReturn(mail);
    when(userDebitedMock.getUserSubscriptionId()).thenReturn(userSubscriptionId);
    when(userServiceMock.getUserById(userOwnerIdentifier)).thenReturn(userMock);
    when(userSubscriptionConfMock.getUserToCreditId()).thenReturn(userOwnerIdentifier);
    when(upcomingUserDebitServiceMock.getUpcomingUserDebited())
        .thenReturn(List.of(userDebitedMock));

    when(stripeCustomerMock.getName()).thenReturn("customer name");
    when(stripeCustomerMock.getCreated()).thenReturn(stripeCreated);
    when(stripeCustomerServiceMock.getCustomer(userMock)).thenReturn(stripeCustomerMock);
    when(customerMock.getName()).thenReturn("customer name");
    when(customerMock.getEmail()).thenReturn(mail);
    when(unknownStripeCustomerMock.getName()).thenReturn("unknown customer name");
    when(unknownStripeCustomerMock.getEmail()).thenReturn("unknown@example.com");
    when(unknownStripeCustomerMock.getStripeCustomerIdentifier())
        .thenReturn(randomUUID().toString());
    when(customerServiceMock.getCustomers(
            eq(userOwnerIdentifier),
            eq(null),
            eq(null),
            eq(mail),
            eq(null),
            eq(null),
            eq(null),
            anyList(),
            eq(null),
            eq(ENABLED),
            eq(new PageFromOne(MIN_PAGE)),
            eq(new BoundedPageSize(MAX_SIZE))))
        .thenReturn(List.of(customerMock));
    when(unknownStripeCustomerJpaRepositoryMock.findAllByCreationDatetimeBetween(any(), any()))
        .thenReturn(List.of(unknownStripeCustomerMock));
    when(customerExportFunctionMock.apply(any())).thenReturn(exportedExcelFileMock);
    when(bucketComponentMock.upload(eq(exportedExcelFileMock), anyString(), eq(true)))
        .thenReturn(mock());
    when(customerExportHistoryJpaRepositoryMock.save(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    assertDoesNotThrow(
        () -> subject.accept(new UpcomingDebitedCustomerExportRequested(YearMonth.of(2026, 5))));

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
}
