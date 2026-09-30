package app.bpartners.api.repository;

import app.bpartners.api.model.UserSubscriptionCommitment;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSubscriptionCommitmentJpaRepository
    extends JpaRepository<UserSubscriptionCommitment, String> {

  List<UserSubscriptionCommitment> findAllByUserId(String userId);

  @Query(
      "select distinct c.userId from user_subscription_commitment c"
          + " where c.commitmentEndDatetime > :now")
  List<String> findUserIdsWithCommitmentEndingAfter(@Param("now") Instant now);
}
