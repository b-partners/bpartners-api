package app.bpartners.api.repository;

import app.bpartners.api.model.Attachment;
import java.util.List;

public interface AttachmentRepository {
  List<Attachment> findByIdInvoiceRelaunch(String idInvoiceRelaunch);

  List<Attachment> saveAll(List<Attachment> attachments, String idInvoiceRelaunch);
}
