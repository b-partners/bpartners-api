package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HCustomerTemplate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerJpaRepository extends JpaRepository<HCustomerTemplate, String> {
  List<HCustomerTemplate> findByIdAccountAndNameContainingIgnoreCase(String idAccount, String name);

  List<HCustomerTemplate> findAllByIdAccount(String idAccount);
}
