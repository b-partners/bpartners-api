package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HAttachment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentJpaRepository extends JpaRepository<HAttachment, String> {
  List<HAttachment> findAllByIdInvoiceRelaunch(String idInvoiceRelaunch);
}
