package app.bpartners.api.service.event;

import static app.bpartners.api.endpoint.rest.model.CustomerStatus.ENABLED;
import static app.bpartners.api.model.BoundedPageSize.MAX_SIZE;
import static app.bpartners.api.model.PageFromOne.MIN_PAGE;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.CustomerExportHistorySaved;
import app.bpartners.api.endpoint.event.model.UpcomingDebitedCustomerExportRequested;
import app.bpartners.api.file.bucket.CustomBucketComponent;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.CustomerExportHistory;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.jpa.CustomerExportHistoryJpaRepository;
import app.bpartners.api.service.customer.CustomerExportPayload;
import app.bpartners.api.service.customer.CustomerService;
import app.bpartners.api.service.file.CustomerExportFunction;
import app.bpartners.api.service.subscription.StripeCustomerService;
import app.bpartners.api.service.subscription.UpcomingUserDebitService;
import app.bpartners.api.service.user.UserService;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpcomingDebitedCustomerExportRequestedService
    implements Consumer<UpcomingDebitedCustomerExportRequested> {
  private final UserService userService;
  private final CustomerService customerService;
  private final UpcomingUserDebitService upcomingUserDebitService;
  private final UserSubscriptionConf userSubscriptionConf;
  private final CustomerExportFunction customerExportFunction;
  private final CustomBucketComponent customBucketComponent;
  private final CustomerExportHistoryJpaRepository customerExportHistoryJpaRepository;
  private final EventProducer eventProducer;
  private final StripeCustomerService stripeCustomerService;

  @Override
  public void accept(UpcomingDebitedCustomerExportRequested event) {
    var userOwnerIdentifier = userSubscriptionConf.getUserToCreditId();
    var user = userService.getUserById(userOwnerIdentifier);

    var upcomingDebitedCustomers = upcomingUserDebitService.getUpcomingDebitedCustomers();

    var customerStatus = ENABLED;
    var page = new PageFromOne(MIN_PAGE);
    var pageSize = new BoundedPageSize(MAX_SIZE);

    var billedCustomers =
        upcomingDebitedCustomers.billedUsers().stream()
            .map(
                upcomingDebitedUser -> {
                  var userSubscriptionId = upcomingDebitedUser.getUserSubscriptionId();
                  var stripeCustomer = stripeCustomerService.getCustomer(upcomingDebitedUser);
                  var customer =
                      customerService
                          .getCustomers(
                              user.getId(),
                              null,
                              null,
                              upcomingDebitedUser.getEmail(),
                              null,
                              null,
                              null,
                              List.of(),
                              null,
                              customerStatus,
                              page,
                              pageSize)
                          .stream()
                          .findAny()
                          .orElse(null);
                  if (customer == null) {
                    return null;
                  }
                  return new CustomerExportPayload(
                      customer.getName(),
                      customer.getEmail(),
                      userSubscriptionId,
                      stripeCustomer.getName(),
                      false,
                      Instant.ofEpochSecond(stripeCustomer.getCreated()));
                })
            .toList();

    var notBilledCustomers =
        upcomingDebitedCustomers.notBilledCustomers().stream()
            .map(
                stripeCustomer ->
                    new CustomerExportPayload(
                        null,
                        stripeCustomer.getEmail(),
                        stripeCustomer.getId(),
                        stripeCustomer.getName(),
                        true,
                        Instant.ofEpochSecond(stripeCustomer.getCreated())))
            .toList();

    var customerExportPayloads =
        Stream.of(notBilledCustomers, billedCustomers).flatMap(List::stream).toList();

    var exportedExcelFile = customerExportFunction.apply(customerExportPayloads);

    var fileKey = "customers/" + randomUUID();

    customBucketComponent.upload(exportedExcelFile, fileKey, true);

    var additionalProperties = new HashMap<String, Object>();

    additionalProperties.put("month", event.getYearMonth().getMonthValue());
    additionalProperties.put("year", event.getYearMonth().getYear());
    additionalProperties.put("usage", "Invoice tracking");

    var savedCustomerExportHistory =
        customerExportHistoryJpaRepository.save(
            CustomerExportHistory.builder()
                .id(randomUUID().toString())
                .userOwnerIdentifier(userOwnerIdentifier)
                .fileKey(fileKey)
                .additionalProperties(additionalProperties)
                .creationDatetime(now())
                .build());

    eventProducer.accept(
        List.of(new CustomerExportHistorySaved(savedCustomerExportHistory.getId())));
  }
}
