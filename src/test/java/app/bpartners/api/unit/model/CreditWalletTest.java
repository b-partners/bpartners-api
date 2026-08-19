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
}
