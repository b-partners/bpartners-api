package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HCustomer;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerJpaRepository extends JpaRepository<HCustomer, String> {
  List<HCustomer> findByIdAccountAndFirstNameAndLastNameContainingIgnoreCase(String idAccount,
                                                                             String firstName,
                                                                             String lastName,
                                                                             Pageable pageable);

  List<HCustomer> findAllByIdAccount(String idAccount, Pageable pageable);
}
