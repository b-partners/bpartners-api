package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HInvoiceRelaunchConf;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRelaunchJpaRepository extends JpaRepository<HInvoiceRelaunchConf, String> {
  Optional<HInvoiceRelaunchConf> getByAccountId(String accountId);
}
