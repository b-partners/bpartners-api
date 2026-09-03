package app.bpartners.api.repository;

import app.bpartners.api.model.EmailRecipient;
import java.util.List;

public interface EmailRecipientRepository {
  List<EmailRecipient> findByAccountHolderId(String accountHolderId);

  List<EmailRecipient> saveAll(String accountHolderId, List<EmailRecipient> recipients);
}
