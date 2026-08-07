package app.bpartners.api.repository;

import app.bpartners.api.model.UserSubscriptionCommitment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSubscriptionCommitmentJpaRepository
    extends JpaRepository<UserSubscriptionCommitment, String> {

  List<UserSubscriptionCommitment> findAllByUserId(String userId);
}
