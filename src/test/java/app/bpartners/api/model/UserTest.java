package app.bpartners.api.model;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.api.model.subscription.SubscriptionProduct;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserTest {
  User subject;

  private static UserSubscriptionProduct userSubscriptionProduct(
      SubscriptionProduct product, Instant creationDatetime, Instant endDatetime) {
    return UserSubscriptionProduct.builder()
        .id(randomUUID().toString())
        .subscriptionProduct(product)
        .creationDatetime(creationDatetime)
        .subscriptionEndDatetime(endDatetime)
        .build();
  }

  @Test
  void get_actual_subscription_product_null_when_no_subscription_products() {
    subject = User.builder().subscriptionProducts(null).build();
    assertNull(subject.getActualSubscriptionProduct());

    subject = User.builder().subscriptionProducts(List.of()).build();
    assertNull(subject.getActualSubscriptionProduct());
  }

  @Test
  void get_actual_subscription_product_null_when_all_subscriptions_ended() {
    var now = Instant.now();
    subject =
        User.builder()
            .subscriptionProducts(
                List.of(
                    userSubscriptionProduct(
                        SubscriptionProduct.builder().id(randomUUID().toString()).build(),
                        now.minus(10, ChronoUnit.DAYS),
                        now.minus(1, ChronoUnit.DAYS))))
            .build();

    assertNull(subject.getActualSubscriptionProduct());
  }

  @Test
  void get_actual_subscription_product_returns_latest_active_by_creation_datetime() {
    var now = Instant.now();
    var oldestActiveProduct = SubscriptionProduct.builder().id(randomUUID().toString()).build();
    var latestActiveProduct = SubscriptionProduct.builder().id(randomUUID().toString()).build();
    var endedProduct = SubscriptionProduct.builder().id(randomUUID().toString()).build();

    subject =
        User.builder()
            .subscriptionProducts(
                List.of(
                    userSubscriptionProduct(
                        oldestActiveProduct, now.minus(5, ChronoUnit.DAYS), null),
                    userSubscriptionProduct(
                        latestActiveProduct, now.minus(1, ChronoUnit.DAYS), null),
                    userSubscriptionProduct(endedProduct, now, now.plus(1, ChronoUnit.DAYS))))
            .build();

    assertEquals(latestActiveProduct, subject.getActualSubscriptionProduct());
  }

  @Test
  void get_default_website() {
    var nonValidAccountHolder = AccountHolder.builder().build();
    var anotherNonValidAccountHolder = AccountHolder.builder().website("").build();
    var firstAccountHolder = AccountHolder.builder().website(randomUUID() + ".com").build();
    var secondAccountHolder = AccountHolder.builder().website(randomUUID() + ".com").build();

    subject =
        User.builder()
            .accountHolders(
                List.of(
                    nonValidAccountHolder,
                    anotherNonValidAccountHolder,
                    firstAccountHolder,
                    secondAccountHolder))
            .build();

    var actual = subject.getDefaultWebsite();

    assertEquals(firstAccountHolder.getWebsite(), actual);
  }

  @Test
  void get_default_website_null_when_no_account_holder_have_website() {
    var nonValidAccountHolder = AccountHolder.builder().build();

    subject = User.builder().accountHolders(List.of(nonValidAccountHolder)).build();

    var actual = subject.getDefaultWebsite();

    assertEquals(null, actual);
  }

  @Test
  void get_default_website_null_when_empty_account_holder() {
    subject = User.builder().accountHolders(null).build();

    var actual = subject.getDefaultWebsite();

    assertEquals(null, actual);
  }
}
