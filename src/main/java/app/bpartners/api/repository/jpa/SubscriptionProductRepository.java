package app.bpartners.api.repository.jpa;

import app.bpartners.api.model.subscription.SubscriptionProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionProductRepository extends JpaRepository<SubscriptionProduct, String> {}
