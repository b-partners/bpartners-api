package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HHasCustomer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HasCustomerJpaRepository extends JpaRepository<HHasCustomer, String> {}
