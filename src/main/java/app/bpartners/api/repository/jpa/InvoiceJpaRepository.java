package app.bpartners.api.repository.jpa;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.repository.jpa.model.HInvoice;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceJpaRepository extends JpaRepository<HInvoice, String> {
  List<HInvoice> findAllByIdAccountAndStatus(
      String idAccount,
      InvoiceStatus status,
      Pageable pageable);


  List<HInvoice> findAllByIdAccount(String idAccount, Pageable pageable);

  List<HInvoice> findByIdAccountAndRefAndStatus(
      String idAccount, String ref, InvoiceStatus status);

  List<HInvoice> findAllByToBeRelaunched(boolean toBeRelaunched);
}
