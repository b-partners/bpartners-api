package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.INVOICED;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PAID;

import app.bpartners.api.model.subscription.SubscriptionInvoicePeriod;
import app.bpartners.api.model.subscription.UserSubscriptionUpgrade;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.jpa.SubscriptionInvoicePeriodRepository;
import app.bpartners.api.repository.jpa.SubscriptionPaymentRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionUpgradeJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LiveSubscriptionInvoicePeriodFinder {
  private final SubscriptionInvoicePeriodRepository subscriptionInvoicePeriodRepository;
  private final SubscriptionPaymentRepository subscriptionPaymentRepository;
  private final UserSubscriptionUpgradeJpaRepository upgradeRepository;
  private final InvoiceRepository invoiceRepository;

  public List<SubscriptionInvoicePeriod> findLive(String userId, Instant asOf) {
    var refundedInvoiceIds =
        Set.copyOf(subscriptionPaymentRepository.findRefundedInvoiceIdsByUserId(userId));
    var supersedingCutoff = latestUpgradeCreationBefore(userId, asOf);
    return subscriptionInvoicePeriodRepository.findByUserId(userId).stream()
        .filter(period -> !refundedInvoiceIds.contains(period.getInvoiceId()))
        .filter(period -> !isSupersededBy(period, supersedingCutoff))
        .toList();
  }

  private Optional<Instant> latestUpgradeCreationBefore(String userId, Instant asOf) {
    return upgradeRepository.findAllByUserIdOrderByCreationDatetimeDesc(userId).stream()
        .filter(upgrade -> upgrade.getStatus() == PAID || upgrade.getStatus() == INVOICED)
        .map(UserSubscriptionUpgrade::getCreationDatetime)
        .filter(Objects::nonNull)
        .filter(creation -> creation.isBefore(asOf))
        .max(Instant::compareTo);
  }

  private boolean isSupersededBy(SubscriptionInvoicePeriod period, Optional<Instant> cutoff) {
    if (cutoff.isEmpty()) {
      return false;
    }
    var invoice = invoiceRepository.findById(period.getInvoiceId());
    return invoice != null
        && invoice.getCreatedAt() != null
        && invoice.getCreatedAt().isBefore(cutoff.get());
  }
}
