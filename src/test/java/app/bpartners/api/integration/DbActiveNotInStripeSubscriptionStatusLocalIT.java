package app.bpartners.api.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.stripe.StripeClient;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.Subscription;
import com.stripe.param.SubscriptionListParams;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Slf4j
@Disabled("TODO: local use only")
class DbActiveNotInStripeSubscriptionStatusLocalIT {
  private static final String API_KEY_ENV = "STRIPE_PRIVATE_API_KEY";
  private static final String INPUT =
      "src/test/resources/tmp/users_db_subscription_active_not_in_stripe.csv";
  private static final String OUTPUT = "build/db-active-not-in-stripe-status.csv";
  private static final int CUSTOMER_ID_COLUMN = 1;
  private static final int DB_EMAIL_COLUMN = 3;
  private static final int DB_PLAN_COLUMN = 5;
  private static final int DB_INTERVAL_COLUMN = 7;
  private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final String HEADER =
      "customerId;dbEmail;dbPlan;dbInterval;stripeCustomerState;subscriptionId;status;"
          + "cancelAtPeriodEnd;canceledAt;endedAt;cancellationReason;currentPeriodEnd;amount";

  @SneakyThrows
  @Test
  void report_stripe_subscription_state_of_users_active_in_db() {
    var client = StripeClient.builder().setApiKey(System.getenv(API_KEY_ENV)).build();
    var lines = new ArrayList<String>();
    lines.add(HEADER);

    for (var row : Files.readAllLines(Path.of(INPUT))) {
      if (row.isBlank()) {
        continue;
      }
      var columns = row.split(",", -1);
      var customerId = columns[CUSTOMER_ID_COLUMN];
      var prefix =
          List.of(
              customerId,
              columns[DB_EMAIL_COLUMN],
              columns[DB_PLAN_COLUMN],
              columns[DB_INTERVAL_COLUMN]);
      lines.addAll(stateOf(client, customerId, prefix));
    }

    Files.createDirectories(Path.of(OUTPUT).getParent());
    Files.write(Path.of(OUTPUT), lines);
    assertFalse(lines.size() <= 1);
    log.info("{}", String.join("\n", lines));
    log.info("CSV written to {}", Path.of(OUTPUT).toAbsolutePath());
  }

  private static List<String> stateOf(StripeClient client, String customerId, List<String> prefix)
      throws Exception {
    String customerState;
    try {
      var customer = client.customers().retrieve(customerId);
      customerState = Boolean.TRUE.equals(customer.getDeleted()) ? "DELETED" : "EXISTS";
    } catch (InvalidRequestException e) {
      return List.of(line(prefix, "NOT_FOUND", "", "", "", "", "", "", "", ""));
    }
    if ("DELETED".equals(customerState)) {
      return List.of(line(prefix, customerState, "", "", "", "", "", "", "", ""));
    }
    var params =
        SubscriptionListParams.builder()
            .setCustomer(customerId)
            .setStatus(SubscriptionListParams.Status.ALL)
            .setLimit(100L)
            .build();
    var rows = new ArrayList<String>();
    for (var subscription : client.subscriptions().list(params).autoPagingIterable()) {
      rows.add(subscriptionLine(prefix, customerState, subscription));
    }
    if (rows.isEmpty()) {
      rows.add(line(prefix, customerState, "NONE", "", "", "", "", "", "", ""));
    }
    return rows;
  }

  private static String subscriptionLine(
      List<String> prefix, String customerState, Subscription subscription) {
    var cancellationReason =
        subscription.getCancellationDetails() == null
            ? ""
            : String.valueOf(subscription.getCancellationDetails().getReason());
    return line(
        prefix,
        customerState,
        subscription.getId(),
        subscription.getStatus(),
        String.valueOf(subscription.getCancelAtPeriodEnd()),
        dateOf(subscription.getCanceledAt()),
        dateOf(subscription.getEndedAt()),
        cancellationReason,
        dateOf(subscription.getCurrentPeriodEnd()),
        amountOf(subscription));
  }

  private static String amountOf(Subscription subscription) {
    var cents =
        subscription.getItems().getData().stream()
            .filter(item -> item.getPrice() != null && item.getPrice().getUnitAmount() != null)
            .mapToLong(
                item ->
                    item.getPrice().getUnitAmount()
                        * (item.getQuantity() == null ? 1L : item.getQuantity()))
            .sum();
    return String.valueOf(cents / 100.0);
  }

  private static String dateOf(Long epochSecond) {
    return epochSecond == null
        ? ""
        : Instant.ofEpochSecond(epochSecond).atZone(PARIS).toLocalDate().format(DATE_FORMAT);
  }

  private static String line(
      List<String> prefix,
      String customerState,
      String subscriptionId,
      String status,
      String cancelAtPeriodEnd,
      String canceledAt,
      String endedAt,
      String cancellationReason,
      String currentPeriodEnd,
      String amount) {
    var all = new ArrayList<>(prefix);
    all.addAll(
        List.of(
            customerState,
            subscriptionId,
            status,
            cancelAtPeriodEnd,
            canceledAt,
            endedAt,
            cancellationReason,
            currentPeriodEnd,
            amount));
    return String.join(";", all);
  }
}
