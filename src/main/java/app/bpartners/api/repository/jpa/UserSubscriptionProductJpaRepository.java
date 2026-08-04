package app.bpartners.api.repository.jpa;

import app.bpartners.api.model.UserSubscriptionProduct;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSubscriptionProductJpaRepository
    extends JpaRepository<UserSubscriptionProduct, String> {
  boolean existsByUserIdAndSubscriptionEndDatetimeIsNull(String userId);

  List<UserSubscriptionProduct> findAllByUserIdAndSubscriptionEndDatetimeIsNull(String userId);
}
