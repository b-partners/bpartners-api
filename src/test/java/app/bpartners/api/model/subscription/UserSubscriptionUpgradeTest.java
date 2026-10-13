package app.bpartners.api.model.subscription;

import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.INVOICED;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PAID;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PENDING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserSubscriptionUpgradeTest {

  @Test
  void has_no_status_without_history() {
    var upgrade = UserSubscriptionUpgrade.builder().id("upgrade_id").build();

    assertTrue(upgrade.getStatusHistory().isEmpty());
    assertNull(upgrade.getStatus());
  }

  @Test
  void appends_each_status_to_the_history_and_exposes_the_latest_one() {
    var upgrade = UserSubscriptionUpgrade.builder().id("upgrade_id").status(PENDING).build();
    var paid = upgrade.toBuilder().status(PAID).build();
    var invoiced = paid.toBuilder().status(INVOICED).build();

    assertEquals(PENDING, upgrade.getStatus());
    assertEquals(PAID, paid.getStatus());
    assertEquals(INVOICED, invoiced.getStatus());
    assertEquals(
        List.of(PENDING, PAID, INVOICED),
        invoiced.getStatusHistory().stream()
            .map(UserSubscriptionUpgradeStatusHistory::getStatus)
            .toList());
    assertEquals(
        "upgrade_id", invoiced.getStatusHistory().getFirst().getUserSubscriptionUpgradeId());
  }

  @Test
  void orders_the_history_by_creation_datetime() {
    var upgrade =
        UserSubscriptionUpgrade.builder()
            .id("upgrade_id")
            .statusHistory(
                List.of(
                    history("later", PAID, "2026-10-12T10:00:00Z"),
                    history("earlier", PENDING, "2026-10-12T09:00:00Z")))
            .build();

    assertEquals(PAID, upgrade.getStatus());
    assertEquals("earlier", upgrade.getStatusHistory().getFirst().getId());
  }

  private static UserSubscriptionUpgradeStatusHistory history(
      String id, SubscriptionUpgradeStatus status, String creation) {
    return UserSubscriptionUpgradeStatusHistory.builder()
        .id(id)
        .status(status)
        .creationDatetime(Instant.parse(creation))
        .build();
  }
}
