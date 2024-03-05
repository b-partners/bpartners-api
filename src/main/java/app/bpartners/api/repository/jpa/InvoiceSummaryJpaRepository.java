package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HInvoiceSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceSummaryJpaRepository extends JpaRepository<HInvoiceSummary, String> {
  HInvoiceSummary findTopByIdUserOrderByUpdatedAtDesc(String idUser);
}
