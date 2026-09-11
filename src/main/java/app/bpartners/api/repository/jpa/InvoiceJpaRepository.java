package app.bpartners.api.repository.jpa;

import app.bpartners.api.endpoint.rest.model.ArchiveStatus;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.subscription.OverageInvoiceLine;
import app.bpartners.api.repository.jpa.model.HInvoice;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceJpaRepository extends JpaRepository<HInvoice, String> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<HInvoice> findOptionalById(String id);

  @Query(
      "select new app.bpartners.api.model.subscription.OverageInvoiceLine("
          + "i.createdDatetime, p.quantity, p.unitPrice, p.vatPercent)"
          + " from HInvoice i join i.products p"
          + " where i.idUser = :systemUserId and p.description = :description"
          + " and i.createdDatetime >= :from and i.createdDatetime < :to")
  List<OverageInvoiceLine> findOverageLines(
      @Param("systemUserId") String systemUserId,
      @Param("description") String description,
      @Param("from") Instant from,
      @Param("to") Instant to);

  List<HInvoice> findAllByIdUserAndArchiveStatusAndTitleContainingIgnoreCaseAndStatusIn(
      String idUser,
      ArchiveStatus archiveStatus,
      String title,
      List<InvoiceStatus> status,
      Pageable pageable);

  List<HInvoice> findAllByIdUserAndArchiveStatusAndTitleContainingIgnoreCase(
      String idUser, ArchiveStatus archiveStatus, String title, Pageable pageable);

  List<HInvoice> findByIdUserAndRef(String idAccount, String ref);

  int countAllByIdUserAndSendingDateBetween(String idUser, LocalDate from, LocalDate to);

  List<HInvoice> findAllByToBeRelaunched(boolean toBeRelaunched);

  List<HInvoice> findAllByIdUserAndArchiveStatus(String idUser, ArchiveStatus archiveStatus);
}
