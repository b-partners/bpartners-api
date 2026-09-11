package app.bpartners.api.repository.jpa;

import app.bpartners.api.model.subscription.SubscriptionPayment;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPayment, String> {
  Optional<SubscriptionPayment> findByStripeInvoiceId(String stripeInvoiceId);

  List<SubscriptionPayment> findByInvoiceIdIsNull();

  List<SubscriptionPayment>
      findByUserIdAndInvoiceIdIsNotNullAndPaymentDatetimeBetweenOrderByPaymentDatetimeDesc(
          String userId, Instant from, Instant to);

  @Query(
      "select p from subscription_payment p"
          + " where p.invoiceId is not null"
          + " and p.paymentDatetime >= :from and p.paymentDatetime < :to")
  List<SubscriptionPayment> findInvoicedBetween(
      @Param("from") Instant from, @Param("to") Instant to);
}
