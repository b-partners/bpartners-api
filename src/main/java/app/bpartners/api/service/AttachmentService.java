package app.bpartners.api.service;

import app.bpartners.api.model.Attachment;
import app.bpartners.api.repository.AttachmentRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AttachmentService {
  private AttachmentRepository repository;

  public List<Attachment> findAllByIdInvoiceRelaunch(String idInvoiceRelaunch) {
    return repository.findByIdInvoiceRelaunch(idInvoiceRelaunch);
  }

  public List<Attachment> saveAll(List<Attachment> attachments, String idInvoiceRelaunch) {
    return repository.saveAll(attachments, idInvoiceRelaunch);
  }
}
