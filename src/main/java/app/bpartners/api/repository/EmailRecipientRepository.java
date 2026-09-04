package app.bpartners.api.repository;

import app.bpartners.api.endpoint.rest.model.EmailRecipientType;
import app.bpartners.api.model.EmailRecipient;
import java.util.List;

public interface EmailRecipientRepository {
  List<EmailRecipient> findByAccountHolderId(String accountHolderId);

  List<EmailRecipient> findByAccountHolderIdAndType(
      String accountHolderId, EmailRecipientType type);

  List<EmailRecipient> saveByType(
      String accountHolderId, EmailRecipientType type, List<EmailRecipient> recipients);
}
