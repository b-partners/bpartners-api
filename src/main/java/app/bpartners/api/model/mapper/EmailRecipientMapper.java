package app.bpartners.api.model.mapper;

import app.bpartners.api.model.EmailRecipient;
import app.bpartners.api.repository.jpa.model.HEmailRecipient;
import org.springframework.stereotype.Component;

@Component
public class EmailRecipientMapper {
  public HEmailRecipient toEntity(EmailRecipient domain) {
    return HEmailRecipient.builder()
        .id(domain.getId())
        .idAccountHolder(domain.getIdAccountHolder())
        .type(domain.getType())
        .email(domain.getEmail())
        .updatedAt(domain.getUpdatedAt())
        .build();
  }

  public EmailRecipient toDomain(HEmailRecipient entity) {
    return EmailRecipient.builder()
        .id(entity.getId())
        .idAccountHolder(entity.getIdAccountHolder())
        .type(entity.getType())
        .email(entity.getEmail())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }
}
