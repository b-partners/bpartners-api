package app.bpartners.api.service.accountholder;

import app.bpartners.api.model.EmailRecipient;
import app.bpartners.api.repository.AccountHolderRepository;
import app.bpartners.api.repository.EmailRecipientRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class EmailRecipientService {
  private final EmailRecipientRepository emailRecipientRepository;
  private final AccountHolderRepository accountHolderRepository;

  public List<EmailRecipient> getByAccountHolderId(String accountHolderId) {
    accountHolderRepository.findById(accountHolderId);
    return emailRecipientRepository.findByAccountHolderId(accountHolderId);
  }

  public List<EmailRecipient> configure(String accountHolderId, List<EmailRecipient> recipients) {
    accountHolderRepository.findById(accountHolderId);
    return emailRecipientRepository.saveAll(accountHolderId, recipients);
  }
}
