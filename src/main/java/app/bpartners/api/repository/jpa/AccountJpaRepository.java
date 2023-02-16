package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountJpaRepository extends JpaRepository<HAccount, String> {
}
