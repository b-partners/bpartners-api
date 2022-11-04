package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.BusinessActivity;
import app.bpartners.api.model.exception.NotFoundException;
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
  private final BusinessActivityMapper domainMapper;

  @Override
  public BusinessActivity save(BusinessActivity businessActivity) {
    HBusinessActivity entity =
        jpaRepository.findByAccountHolder_Id(businessActivity.getAccountHolder().getId())
            .orElseGet(
                () -> domainMapper.toEntity(businessActivity,
                    businessActivity.getAccountHolder().getAccountId())
            );

    Optional<HBusinessActivityTemplate> template1 =
        templateRepository.findByNameIgnoreCase(businessActivity.getPrimaryActivity());
    Optional<HBusinessActivityTemplate> template2 =
        templateRepository.findByNameIgnoreCase(businessActivity.getSecondaryActivity());

    template1.ifPresent(template -> {
      entity.setPrimaryActivity(template);
      entity.setOtherPrimaryActivity(null);
    });

    template2.ifPresent(template -> {
      entity.setSecondaryActivity(template);
      entity.setOtherSecondaryActivity(null);
    });

    if (template1.isEmpty()) {
      entity.setOtherPrimaryActivity(businessActivity.getPrimaryActivity());
    }
    if (template2.isEmpty()) {
      entity.setOtherSecondaryActivity(businessActivity.getSecondaryActivity());
    }

    return domainMapper.toDomain(jpaRepository.save(entity));
  }

  @Override
  public BusinessActivity findByAccountHolderId(String accountHolderId) {
    return domainMapper.toDomain(
        jpaRepository.findByAccountHolder_Id(accountHolderId)
            .orElseThrow(
                () -> new NotFoundException(
                    "No Business Activity found for AccountHolder." + accountHolderId))
    );
  }
}
