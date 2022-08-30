package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HPreUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PreUserJpaRepository extends JpaRepository<HPreUser, String> {
}
