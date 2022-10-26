package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HInvoiceRelaunch;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRelaunchJpaRepository extends JpaRepository<HInvoiceRelaunch, String> {
  Optional<HInvoiceRelaunch> getByAccountId(String accountId);
}
