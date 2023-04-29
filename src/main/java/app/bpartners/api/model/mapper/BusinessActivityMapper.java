package app.bpartners.api.model.mapper;

import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.BusinessActivity;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import app.bpartners.api.repository.jpa.model.HBusinessActivity;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class BusinessActivityMapper {
  public HBusinessActivity toEntity(BusinessActivity domain, HAccountHolder accountHolder) {
    return HBusinessActivity.builder()
        .id(domain.getId())
        .accountHolder(accountHolder)
        .build();
  }

  public BusinessActivity toDomain(HBusinessActivity entity, AccountHolder accountHolder) {
    String primaryActivity = entity.getPrimaryActivity() == null ? null :
        entity.getPrimaryActivity().getName();
    String secondaryActivity = entity.getSecondaryActivity() == null ? null :
        entity.getSecondaryActivity().getName();
    BusinessActivity activity = BusinessActivity.builder()
        .id(entity.getId())
        .accountHolder(accountHolder)
        .primaryActivity(primaryActivity)
        .secondaryActivity(secondaryActivity)
        .build();

    if (entity.getOtherPrimaryActivity() != null) {
      activity.setPrimaryActivity(entity.getOtherPrimaryActivity());
    }
    if (entity.getOtherSecondaryActivity() != null) {
      activity.setSecondaryActivity(entity.getOtherSecondaryActivity());
    }
    return activity;
  }
}
