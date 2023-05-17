package app.bpartners.api.model.mapper;

import app.bpartners.api.model.Feedback;
import app.bpartners.api.repository.jpa.model.HFeedback;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class FeedbackMapper {
  private final AccountHolderMapper accountHolderMapper;
  private final CustomerMapper customerMapper;

  public Feedback toDomain(HFeedback entity) {
    if (entity == null) {
      return null;
    }
    return Feedback.builder()
        .id(entity.getId())
        .accountHolder(accountHolderMapper.toDomain(entity.getAccountHolder()))
        .customers(entity.getCustomers().stream()
            .map(customerMapper::toDomain)
            .collect(Collectors.toList()))
        .creationDatetime(entity.getCreationDatetime())
        .build();
  }

  public HFeedback toEntity(Feedback domain) {
    return HFeedback.builder()
        .id(domain.getId())
        .accountHolder(accountHolderMapper.toEntity(domain.getAccountHolder()))
        .customers(domain.getCustomers().stream()
            .map(customerMapper::toEntity)
            .collect(Collectors.toList()))
        .creationDatetime(domain.getCreationDatetime())
        .build();
  }
}
