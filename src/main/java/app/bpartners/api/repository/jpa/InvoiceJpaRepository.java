package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HInvoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceJpaRepository extends JpaRepository<HInvoice, String> {
}
