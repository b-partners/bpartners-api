package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HCustomer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerJpaRepository extends JpaRepository<HCustomer, String> {
  Optional<HCustomer> findByIdUserAndEmail(String idUser, String email);
}
