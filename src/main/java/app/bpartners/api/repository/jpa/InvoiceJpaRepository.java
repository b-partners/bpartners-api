package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HInvoice;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceJpaRepository extends JpaRepository<HInvoice, String> {
  List<HInvoice> findHInvoicesByIdAccount(String idAccount, Pageable pageable);
}
