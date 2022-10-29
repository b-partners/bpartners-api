package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HInvoiceRelaunch;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRelaunchJpaRepository extends JpaRepository<HInvoiceRelaunch, String> {
  List<HInvoiceRelaunch> findAllByInvoiceId(
      String invoiceId,
      Pageable pageable
  );

  List<HInvoiceRelaunch> findAllByInvoiceIdAndUserRelaunched(
      String invoiceId,
      Pageable pageable,
      boolean isUserRelaunched
  );

}