package app.bpartners.api.repository;

import app.bpartners.api.model.InvoiceSummary;
import java.util.List;

public interface InvoiceSummaryRepository {
  List<InvoiceSummary> saveAll(List<InvoiceSummary> toSave);

  InvoiceSummary findTopByIdUser(String idUser);
}
