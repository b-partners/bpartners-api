package app.bpartners.api.repository.jpa;

import app.bpartners.api.model.UserIgnoredTrialPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserIgnoredTrialPeriodJpaRepository
    extends JpaRepository<UserIgnoredTrialPeriod, String> {
  boolean existsByUserId(String userId);
}
