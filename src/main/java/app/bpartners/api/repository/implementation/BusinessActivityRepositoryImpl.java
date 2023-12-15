package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.BusinessActivity;
import app.bpartners.api.model.mapper.AccountHolderMapper;
import app.bpartners.api.model.mapper.BusinessActivityMapper;
import app.bpartners.api.repository.BusinessActivityRepository;
import app.bpartners.api.repository.jpa.BusinessActivityJpaRepository;
import app.bpartners.api.repository.jpa.BusinessActivityTemplateJpaRepository;
import app.bpartners.api.repository.jpa.model.HBusinessActivity;
import app.bpartners.api.repository.jpa.model.HBusinessActivityTemplate;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@AllArgsConstructor
@Repository
public class BusinessActivityRepositoryImpl implements BusinessActivityRepository {
  private final BusinessActivityJpaRepository jpaRepository;
  private final BusinessActivityTemplateJpaRepository templateRepository;
  private final BusinessActivityMapper mapper;
  private final AccountHolderMapper accountHolderMapper;

  @Override
  public BusinessActivity save(BusinessActivity businessActivity) {
    HBusinessActivity entity =
        jpaRepository
            .findByAccountHolder_Id(businessActivity.getAccountHolder().getId())
            .orElse(
                mapper.toEntity(
                    businessActivity,
                    accountHolderMapper.toEntity(businessActivity.getAccountHolder())));

    Optional<HBusinessActivityTemplate> optionalPrimary =
        templateRepository.findByNameIgnoreCase(businessActivity.getPrimaryActivity());
    Optional<HBusinessActivityTemplate> optionalSecondary =
        templateRepository.findByNameIgnoreCase(businessActivity.getSecondaryActivity());

    optionalPrimary.ifPresent(
        activity -> {
          entity.setPrimaryActivity(activity);
          entity.setOtherPrimaryActivity(null);
        });

    optionalSecondary.ifPresent(
        template -> {
          entity.setSecondaryActivity(template);
          entity.setOtherSecondaryActivity(null);
        });

    if (optionalPrimary.isEmpty()) {
      entity.setOtherPrimaryActivity(businessActivity.getPrimaryActivity());
    }
    if (optionalSecondary.isEmpty()) {
      entity.setOtherSecondaryActivity(businessActivity.getSecondaryActivity());
    }

    HBusinessActivity savedBusinessActivity = jpaRepository.save(entity);

    return mapper.toDomain(
        savedBusinessActivity,
        accountHolderMapper.toDomain(savedBusinessActivity.getAccountHolder()));
  }

  @Override
  public BusinessActivity findByAccountHolderId(String accountHolderId) {
    Optional<HBusinessActivity> optionalActivity =
        jpaRepository.findByAccountHolder_Id(accountHolderId);
    return optionalActivity.isEmpty()
        ? null
        : mapper.toDomain(
            optionalActivity.get(),
            accountHolderMapper.toDomain(optionalActivity.get().getAccountHolder()));
  }
}
