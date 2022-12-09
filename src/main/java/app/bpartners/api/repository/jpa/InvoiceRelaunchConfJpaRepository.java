package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HInvoiceRelaunchConf;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRelaunchConfJpaRepository
    extends JpaRepository<HInvoiceRelaunchConf, String> {
  Optional<HInvoiceRelaunchConf> findByIdInvoice(String idInvoice);
}
