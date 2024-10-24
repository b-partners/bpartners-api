package app.bpartners.api.repository;

import app.bpartners.api.endpoint.rest.model.ArchiveStatus;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.ArchiveInvoice;
import app.bpartners.api.model.Invoice;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository {
  List<Invoice> findAllEnabledByIdUser(String idUser);

  Invoice save(Invoice toSave);

  Invoice getById(String id);

  Optional<Invoice> pwFindOptionalById(String id);

  List<Invoice> findAllByIdUserAndCriteria(
      String idUser,
      List<InvoiceStatus> statusList,
      ArchiveStatus archiveStatus,
      List<String> filters,
      int page,
      int pageSize);

  List<Invoice> archiveAll(List<ArchiveInvoice> archiveInvoices);

  List<Invoice> findByIdUserAndRef(String idUser, String reference);

  Invoice findById(String id);
}
