package app.bpartners.api.repository.jpa;

import app.bpartners.api.endpoint.rest.model.EmailRecipientType;
import app.bpartners.api.repository.jpa.model.HEmailRecipient;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailRecipientJpaRepository extends JpaRepository<HEmailRecipient, String> {
  List<HEmailRecipient> findByIdAccountHolder(String idAccountHolder);

  List<HEmailRecipient> findByIdAccountHolderAndType(
      String idAccountHolder, EmailRecipientType type);

  void deleteByIdAccountHolderAndType(String idAccountHolder, EmailRecipientType type);
}
