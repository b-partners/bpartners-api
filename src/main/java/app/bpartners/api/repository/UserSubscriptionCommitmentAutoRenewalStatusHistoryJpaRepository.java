package app.bpartners.api.repository;

import app.bpartners.api.model.UserSubscriptionCommitmentAutoRenewalStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSubscriptionCommitmentAutoRenewalStatusHistoryJpaRepository
    extends JpaRepository<UserSubscriptionCommitmentAutoRenewalStatusHistory, String> {}
