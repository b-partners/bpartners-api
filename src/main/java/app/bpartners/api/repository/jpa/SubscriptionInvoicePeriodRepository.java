package app.bpartners.api.repository.jpa;

import app.bpartners.api.model.subscription.SubscriptionInvoicePeriod;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionInvoicePeriodRepository
    extends JpaRepository<SubscriptionInvoicePeriod, String> {
  List<SubscriptionInvoicePeriod> findByUserId(String userId);

  Optional<SubscriptionInvoicePeriod> findByInvoiceId(String invoiceId);
}
