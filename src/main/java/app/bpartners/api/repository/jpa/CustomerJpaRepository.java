package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HCustomer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerJpaRepository extends JpaRepository<HCustomer, String> {
  List<HCustomer> findByIdAccountAndFirstNameAndLastNameContainingIgnoreCase(String idAccount,
                                                                             String firstName,
                                                                             String lastName);

  List<HCustomer> findAllByIdAccount(String idAccount);
}
