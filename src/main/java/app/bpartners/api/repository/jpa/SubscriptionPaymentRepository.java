package app.bpartners.api.repository.jpa;

import app.bpartners.api.model.subscription.SubscriptionPayment;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPayment, String> {
  Optional<SubscriptionPayment> findByStripeInvoiceId(String stripeInvoiceId);

  List<SubscriptionPayment> findByInvoiceIdIsNull();

  List<SubscriptionPayment>
      findByUserIdAndInvoiceIdIsNotNullAndPaymentDatetimeBetweenOrderByPaymentDatetimeDesc(
          String userId, Instant from, Instant to);
}
