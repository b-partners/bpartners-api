package app.bpartners.api.integration;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.stripe.StripeClient;
import com.stripe.model.Customer;
import com.stripe.model.Subscription;
import com.stripe.param.SubscriptionListParams;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Slf4j
@Disabled("TODO : local use only")
class ActiveStripeSubscriptionsLocalIT {
  private static final String API_KEY_ENV = "STRIPE_PRIVATE_API_KEY";
  private static final BigDecimal ANNUAL_THRESHOLD_IN_EUROS = BigDecimal.valueOf(199);
  private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final String CSV_HEADER =
      "customerId;email;amount;offer;billingInterval;nextDebitDate";
  private static final Map<Long, String> OFFER_BY_AMOUNT_IN_CENTS =
      Map.ofEntries(
          Map.entry(4900L, "ESSENTIEL"),
          Map.entry(5880L, "ESSENTIEL"),
          Map.entry(52900L, "ESSENTIEL"),
          Map.entry(63480L, "ESSENTIEL"),
          Map.entry(9900L, "PRO"),
          Map.entry(11880L, "PRO"),
          Map.entry(106900L, "PRO"),
          Map.entry(128280L, "PRO"),
          Map.entry(19900L, "EXPERT"),
          Map.entry(23880L, "EXPERT"),
          Map.entry(214900L, "EXPERT"),
          Map.entry(257880L, "EXPERT"));

  private record ActiveSubscription(
      String customerId,
      String email,
      BigDecimal amount,
      String offer,
      String billingInterval,
      String nextDebitDate) {
    String toCsvLine() {
      return String.join(
          ";", customerId, email, amount.toPlainString(), offer, billingInterval, nextDebitDate);
    }
  }

  @SneakyThrows
  @Test
  void list_active_stripe_subscriptions() {
    var client = StripeClient.builder().setApiKey(System.getenv(API_KEY_ENV)).build();
    var params =
        SubscriptionListParams.builder()
            .setStatus(SubscriptionListParams.Status.ACTIVE)
            .addExpand("data.customer")
            .setLimit(100L)
            .build();

    var rows = new ArrayList<ActiveSubscription>();
    for (var subscription : client.subscriptions().list(params).autoPagingIterable()) {
      rows.add(toRow(subscription));
    }

    assertNotNull(rows);
    var lines = new ArrayList<String>();
    lines.add(CSV_HEADER);
    rows.stream().map(ActiveSubscription::toCsvLine).forEach(lines::add);
    var csv = Path.of("build", "active-stripe-subscriptions.csv");
    Files.createDirectories(csv.getParent());
    Files.write(csv, lines);

    log.info("Active Stripe subscriptions: {}", rows.size());
    log.info("{}\n{}", CSV_HEADER, String.join("\n", lines.subList(1, lines.size())));
    log.info("CSV written to {}", csv.toAbsolutePath());
  }

  private static ActiveSubscription toRow(Subscription subscription) {
    var customer = subscription.getCustomerObject();
    var amountInCents = amountInCentsOf(subscription);
    var amount =
        BigDecimal.valueOf(amountInCents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    var billingInterval = amount.compareTo(ANNUAL_THRESHOLD_IN_EUROS) > 0 ? "ANNUAL" : "MONTHLY";
    return new ActiveSubscription(
        customerIdOf(subscription, customer),
        customer == null || customer.getEmail() == null ? "" : customer.getEmail(),
        amount,
        OFFER_BY_AMOUNT_IN_CENTS.getOrDefault(amountInCents, "UNKNOWN"),
        billingInterval,
        nextDebitDateOf(subscription));
  }

  private static String customerIdOf(Subscription subscription, Customer customer) {
    return customer == null ? subscription.getCustomer() : customer.getId();
  }

  private static long amountInCentsOf(Subscription subscription) {
    return subscription.getItems().getData().stream()
        .filter(item -> item.getPrice() != null && item.getPrice().getUnitAmount() != null)
        .mapToLong(
            item ->
                item.getPrice().getUnitAmount()
                    * (item.getQuantity() == null ? 1L : item.getQuantity()))
        .sum();
  }

  private static String nextDebitDateOf(Subscription subscription) {
    if (Boolean.TRUE.equals(subscription.getCancelAtPeriodEnd())
        || subscription.getCurrentPeriodEnd() == null) {
      return "";
    }
    return Instant.ofEpochSecond(subscription.getCurrentPeriodEnd())
        .atZone(PARIS)
        .toLocalDate()
        .format(DATE_FORMAT);
  }
}
