package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HUserInvoiceRelaunchConf;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountInvoiceRelaunchConfJpaRepository
    extends JpaRepository<HUserInvoiceRelaunchConf, String> {
  Optional<HUserInvoiceRelaunchConf> findByIdUser(String idUser);
}
