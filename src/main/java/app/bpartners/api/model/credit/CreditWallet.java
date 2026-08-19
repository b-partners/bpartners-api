package app.bpartners.api.model.credit;

import static java.util.Comparator.naturalOrder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public final class CreditWallet {
  private final List<Lot> liveLots;
  private final Instant updatedAt;

  private CreditWallet(List<Lot> liveLots, Instant updatedAt) {
    this.liveLots = liveLots;
    this.updatedAt = updatedAt;
  }

  public static CreditWallet of(List<CreditTransaction> transactions, Instant at) {
    var liveLots =
        transactions.stream()
            .filter(CreditTransaction::isCredit)
            .filter(transaction -> !transaction.isExpiredAt(at))
            .map(Lot::new)
            .sorted(Lot.BY_EXPIRY_SOONEST_FIRST)
            .toList();
    var debitTotal =
        transactions.stream()
            .filter(transaction -> !transaction.isCredit())
            .mapToLong(CreditTransaction::creditsOrZero)
            .sum();
    var lastMovementDatetime =
        transactions.stream()
            .map(CreditTransaction::getCreationDatetime)
            .filter(Objects::nonNull)
            .max(naturalOrder())
            .orElse(null);
    return new CreditWallet(allocateDebits(liveLots, debitTotal), lastMovementDatetime);
  }

  private static List<Lot> allocateDebits(List<Lot> lots, long debitTotal) {
    var allocated = new ArrayList<Lot>(lots.size());
    var remainingDebit = debitTotal;
    for (var lot : lots) {
      var applied = Math.min(lot.getRemaining(), remainingDebit);
      remainingDebit -= applied;
      allocated.add(lot.reduceBy(applied));
    }
    return allocated;
  }

  public long grantedCredits() {
    return remainingWhere(Lot::isGranted);
  }

  public long purchasedCredits() {
    return remainingWhere(lot -> !lot.isGranted());
  }

  public long spendableCredits() {
    return remainingWhere(lot -> true);
  }

  public long estimatedAnalyses(long creditCostPerAnalysis) {
    return creditCostPerAnalysis <= 0 ? 0 : spendableCredits() / creditCostPerAnalysis;
  }

  public List<CreditExpiration> upcomingExpirations() {
    return liveLots.stream()
        .filter(Lot::hasRemaining)
        .filter(Lot::expires)
        .map(Lot::toExpiration)
        .toList();
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  private long remainingWhere(Predicate<Lot> filter) {
    return liveLots.stream().filter(filter).mapToLong(Lot::getRemaining).sum();
  }
}
