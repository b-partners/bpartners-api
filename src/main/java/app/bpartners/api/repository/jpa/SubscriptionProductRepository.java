package app.bpartners.api.repository.jpa;

import app.bpartners.api.model.subscription.SubscriptionConsumptionType;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionProductRepository extends JpaRepository<SubscriptionProduct, String> {
  SubscriptionProduct findByConsumptionTypeAttached(SubscriptionConsumptionType consumptionType);

  Optional<SubscriptionProduct> findByE2Id(String e2Id);

  List<SubscriptionProduct> findAllByBillingTypeNotNull(Pageable pageable);
}
