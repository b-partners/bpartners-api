package app.bpartners.api.repository.jpa;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.repository.jpa.model.HInvoice;
import java.util.List;
import java.util.Optional;
import javax.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface InvoiceJpaRepository extends JpaRepository<HInvoice, String> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<HInvoice> findOptionalById(String id);

  List<HInvoice> findAllByIdUserAndStatus(
      String idUser,
      InvoiceStatus status,
      Pageable pageable);

  List<HInvoice> findAllByIdUser(String idUser, Pageable pageable);

  List<HInvoice> findByIdUserAndRefAndStatus(
      String idAccount, String ref, InvoiceStatus status);

  List<HInvoice> findAllByToBeRelaunched(boolean toBeRelaunched);
}
