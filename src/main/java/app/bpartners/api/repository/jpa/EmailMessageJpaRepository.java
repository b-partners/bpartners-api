package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HEmailMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailMessageJpaRepository extends JpaRepository<HEmailMessage, String> {
  HEmailMessage getByIdAccount(String idAccount);
}
