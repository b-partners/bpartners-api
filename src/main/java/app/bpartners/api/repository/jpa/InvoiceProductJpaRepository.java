package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HInvoiceProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceProductJpaRepository extends JpaRepository<HInvoiceProduct, String> {
  HInvoiceProduct findTopByIdInvoiceOrderByCreatedDatetimeDesc(String idInvoice);
}
