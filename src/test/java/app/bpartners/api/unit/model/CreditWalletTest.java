package app.bpartners.api.unit.model;

import static app.bpartners.api.model.credit.CreditOrigin.SUBSCRIPTION_GRANT;
import static app.bpartners.api.model.credit.CreditTransactionMovementType.CREDIT;
import static app.bpartners.api.model.credit.CreditTransactionMovementType.DEBIT;
import static app.bpartners.api.model.credit.CreditTransactionType.CONSUMPTION;
import static app.bpartners.api.model.credit.CreditTransactionType.PURCHASE;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.api.model.credit.CreditTransaction;
import app.bpartners.api.model.credit.CreditTransactionMovementType;
import app.bpartners.api.model.credit.CreditTransactionType;
import app.bpartners.api.model.credit.CreditWallet;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class CreditWalletTest {
  Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);

  private static CreditTransaction tx(
      CreditTransactionType type,
      CreditTransactionMovementType movementType,
      long credits,
      Instant expiry,
      Instant creation) {
    return CreditTransaction.builder()
        .type(type)
        .movementType(movementType)
        .credits(credits)
        .expirationDatetime(expiry)
        .creationDatetime(creation)
        .build();
  }

  @Test
  void empty_wallet_is_all_zero() {
    var wallet = CreditWallet.of(List.of(), now);

    assertEquals(0L, wallet.spendableCredits());
    assertEquals(0L, wallet.grantedCredits());
    assertEquals(0L, wallet.purchasedCredits());
    assertEquals(0L, wallet.estimatedAnalyses(2L));
    assertTrue(wallet.upcomingExpirations().isEmpty());
  }

  @Test
  void debits_are_allocated_to_the_soonest_expiring_lot_first() {
    var grantExpiry = now.plus(10, DAYS);
    var wallet =
        CreditWallet.of(
            List.of(
                tx(
                    CreditTransactionType.SUBSCRIPTION_GRANT,
                    CREDIT,
                    20L,
                    grantExpiry,
                    now.minus(2, DAYS)),
                tx(PURCHASE, CREDIT, 30L, null, now.minus(1, DAYS)),
                tx(CONSUMPTION, DEBIT, 5L, null, now)),
            now);

    assertEquals(15L, wallet.grantedCredits());
    assertEquals(30L, wallet.purchasedCredits());
    assertEquals(45L, wallet.spendableCredits());
    assertEquals(22L, wallet.estimatedAnalyses(2L));
    assertEquals(now, wallet.updatedAt());
    assertEquals(1, wallet.upcomingExpirations().size());
    assertEquals(15L, wallet.upcomingExpirations().getFirst().getCredits());
    assertEquals(SUBSCRIPTION_GRANT, wallet.upcomingExpirations().getFirst().getOrigin());
  }

  @Test
  void expired_lots_are_excluded() {
    var wallet =
        CreditWallet.of(
            List.of(
                tx(CreditTransactionType.SUBSCRIPTION_GRANT, CREDIT, 20L, now.minus(1, DAYS), now)),
            now);

    assertEquals(0L, wallet.spendableCredits());
    assertTrue(wallet.upcomingExpirations().isEmpty());
  }

  @Test
  void a_fresh_grant_is_not_reduced_by_consumption_charged_to_an_already_expired_grant() {
    var wallet =
        CreditWallet.of(
            List.of(
                tx(
                    CreditTransactionType.SUBSCRIPTION_GRANT,
                    CREDIT,
                    25L,
                    now.minus(2, DAYS),
                    now.minus(40, DAYS)),
                tx(CONSUMPTION, DEBIT, 8L, null, now.minus(35, DAYS)),
                tx(
                    CreditTransactionType.SUBSCRIPTION_GRANT,
                    CREDIT,
                    25L,
                    now.plus(28, DAYS),
                    now.minus(1, DAYS))),
            now);

    assertEquals(25L, wallet.spendableCredits());
    assertEquals(25L, wallet.grantedCredits());
    assertEquals(1, wallet.upcomingExpirations().size());
    assertEquals(25L, wallet.upcomingExpirations().getFirst().getCredits());
  }

  @Test
  void consumption_reduces_the_current_grant_once_the_previous_one_has_expired() {
    var wallet =
        CreditWallet.of(
            List.of(
                tx(
                    CreditTransactionType.SUBSCRIPTION_GRANT,
                    CREDIT,
                    25L,
                    now.minus(2, DAYS),
                    now.minus(40, DAYS)),
                tx(
                    CreditTransactionType.SUBSCRIPTION_GRANT,
                    CREDIT,
                    25L,
                    now.plus(28, DAYS),
                    now.minus(1, DAYS)),
                tx(CONSUMPTION, DEBIT, 5L, null, now)),
            now);

    assertEquals(20L, wallet.spendableCredits());
    assertEquals(20L, wallet.grantedCredits());
  }
}
