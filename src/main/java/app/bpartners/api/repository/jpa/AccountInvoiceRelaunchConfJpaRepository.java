package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HAccountInvoiceRelaunchConf;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountInvoiceRelaunchConfJpaRepository
    extends JpaRepository<HAccountInvoiceRelaunchConf, String> {
  Optional<HAccountInvoiceRelaunchConf> getByAccountId(String accountId);
}
