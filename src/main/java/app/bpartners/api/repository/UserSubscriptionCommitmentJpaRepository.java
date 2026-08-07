package app.bpartners.api.repository;

import app.bpartners.api.model.UserSubscriptionCommitment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSubscriptionCommitmentJpaRepository
    extends JpaRepository<UserSubscriptionCommitment, String> {}
