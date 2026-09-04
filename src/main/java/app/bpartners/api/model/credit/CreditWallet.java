package app.bpartners.api.model.credit;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;
import static java.util.Comparator.nullsLast;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public final class CreditWallet {
  private static final Comparator<CreditTransaction> BY_CHRONOLOGICAL_ORDER =
      comparing(CreditTransaction::getCreationDatetime, nullsFirst(naturalOrder()))
          .thenComparing(transaction -> transaction.isCredit() ? 0 : 1);

  private final List<Lot> liveLots;
  private final Instant updatedAt;

  private CreditWallet(List<Lot> liveLots, Instant updatedAt) {
    this.liveLots = liveLots;
    this.updatedAt = updatedAt;
  }

  public static CreditWallet of(List<CreditTransaction> transactions, Instant at) {
    var openLots = new ArrayList<WorkingLot>();
    transactions.stream()
        .sorted(BY_CHRONOLOGICAL_ORDER)
        .forEach(transaction -> replay(openLots, transaction));
    var liveLots =
        openLots.stream()
            .filter(lot -> lot.liveAt(at))
            .sorted(comparing(WorkingLot::expirationDatetime, nullsLast(naturalOrder())))
            .map(WorkingLot::toLot)
            .toList();
    var lastMovementDatetime =
        transactions.stream()
            .map(CreditTransaction::getCreationDatetime)
            .filter(Objects::nonNull)
            .max(naturalOrder())
            .orElse(null);
    return new CreditWallet(liveLots, lastMovementDatetime);
  }

  private static void replay(List<WorkingLot> openLots, CreditTransaction transaction) {
    if (transaction.isCredit()) {
      openLots.add(new WorkingLot(transaction));
      return;
    }
    allocateDebitAt(openLots, transaction.creditsOrZero(), transaction.getCreationDatetime());
  }

  private static void allocateDebitAt(List<WorkingLot> openLots, long debit, Instant at) {
    var remainingDebit = debit;
    var candidates =
        openLots.stream()
            .filter(lot -> lot.availableAt(at) && lot.remaining > 0)
            .sorted(comparing(WorkingLot::expirationDatetime, nullsLast(naturalOrder())))
            .toList();
    for (var lot : candidates) {
      if (remainingDebit <= 0) {
        break;
      }
      var applied = Math.min(lot.remaining, remainingDebit);
      lot.remaining -= applied;
      remainingDebit -= applied;
    }
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

  private static final class WorkingLot {
    private final CreditOrigin origin;
    private final Instant expirationDatetime;
    private long remaining;

    private WorkingLot(CreditTransaction transaction) {
      this.origin = CreditOrigin.ofTransactionType(transaction.getType());
      this.expirationDatetime = transaction.getExpirationDatetime();
      this.remaining = transaction.creditsOrZero();
    }

    private Instant expirationDatetime() {
      return expirationDatetime;
    }

    private boolean liveAt(Instant at) {
      return expirationDatetime == null || expirationDatetime.isAfter(at);
    }

    private boolean availableAt(Instant at) {
      return at == null || liveAt(at);
    }

    private Lot toLot() {
      return Lot.of(origin, expirationDatetime, remaining);
    }
  }
}
