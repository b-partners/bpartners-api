package app.bpartners.api.repository;

import app.bpartners.api.model.PreRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PreRegistrationRepository extends JpaRepository<PreRegistration, String> {
}
