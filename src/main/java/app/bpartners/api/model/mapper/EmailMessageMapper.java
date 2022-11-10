package app.bpartners.api.model.mapper;

import app.bpartners.api.model.EmailMessage;
import app.bpartners.api.repository.jpa.model.HEmailMessage;
import org.springframework.stereotype.Component;

@Component
public class EmailMessageMapper {
  public EmailMessage toDomain(HEmailMessage entity) {
    return EmailMessage.builder()
        .id(entity.getId())
        .message(entity.getMessage())
        .idAccount(entity.getIdAccount())
        .build();
  }
}
