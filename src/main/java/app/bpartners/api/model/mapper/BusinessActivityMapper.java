package app.bpartners.api.model.mapper;

import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.BusinessActivity;
import app.bpartners.api.repository.AccountHolderRepository;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import app.bpartners.api.repository.jpa.model.HBusinessActivity;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class BusinessActivityMapper {
  private final AccountHolderJpaRepository accountHolderJpaRepository;
  private final AccountHolderRepository accountHolderRepository;

  public HBusinessActivity toEntity(BusinessActivity domain, String accountId) {
    HAccountHolder persistedAccountHolder =
        accountHolderJpaRepository.findAllByAccountId(accountId).get(0);
    return HBusinessActivity.builder()
        .id(domain.getId())
        .accountHolder(persistedAccountHolder)
        .build();
  }

  public BusinessActivity toDomain(HBusinessActivity entity) {
    if (entity == null) {
      return null;
    }
    AccountHolder authenticatedAccountHolder = accountHolderRepository.findAllByAccountId(
        entity.getAccountHolder().getAccountId()).get(0);
    String primaryActivity = entity.getPrimaryActivity() == null ? null :
        entity.getPrimaryActivity().getName();
    String secondaryActivity = entity.getSecondaryActivity() == null ? null :
        entity.getSecondaryActivity().getName();
    BusinessActivity activity = BusinessActivity.builder()
        .id(entity.getId())
        .accountHolder(authenticatedAccountHolder)
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
