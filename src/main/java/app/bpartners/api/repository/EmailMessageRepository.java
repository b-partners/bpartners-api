package app.bpartners.api.repository;

import app.bpartners.api.model.EmailMessage;

public interface EmailMessageRepository {
  EmailMessage getByAccountId(String accountId);

  EmailMessage getDefaultMessage();
}
