package app.bpartners.api.repository;

import app.bpartners.api.endpoint.rest.model.ArchiveStatus;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.ArchiveInvoice;
import app.bpartners.api.model.Invoice;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository {
  List<Invoice> findAllEnabledByIdUser(String idUser);

  Invoice save(Invoice toSave);

  Invoice getById(String id);

  Optional<Invoice> pwFindOptionalById(String id);

  List<Invoice> findAllByIdUserAndSendingDateBetweenAndPaginate(
      String idUser, LocalDate from, LocalDate to, int page, int pageSize);

  List<Invoice> findAllByIdUserAndCriteria(
      String idUser,
      List<InvoiceStatus> statusList,
      ArchiveStatus archiveStatus,
      List<String> filters,
      Integer page,
      Integer pageSize);

  int countAllByIdUserAndSendingDateBetween(String userId, LocalDate from, LocalDate to);

  List<Invoice> archiveAll(List<ArchiveInvoice> archiveInvoices);

  List<Invoice> findByIdUserAndRef(String idUser, String reference);

  Invoice findById(String id);
}
