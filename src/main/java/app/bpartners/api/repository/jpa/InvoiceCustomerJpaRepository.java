package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HInvoiceCustomer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceCustomerJpaRepository extends JpaRepository<HInvoiceCustomer, String> {
}
