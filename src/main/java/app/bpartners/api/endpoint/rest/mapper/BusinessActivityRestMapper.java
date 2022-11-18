package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.BusinessActivity;
import app.bpartners.api.endpoint.rest.model.CompanyBusinessActivity;
import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.endpoint.rest.security.principal.PrincipalProvider;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.repository.AccountHolderRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class BusinessActivityRestMapper {
  private final AccountHolderRepository accountHolderRepository;
  private final PrincipalProvider provider;

  public BusinessActivity toRest(app.bpartners.api.model.BusinessActivityTemplate domain) {
    return new BusinessActivity()
        .id(domain.getId())
        .name(domain.getName());
  }

  public app.bpartners.api.model.BusinessActivity toDomain(CompanyBusinessActivity rest) {
    AccountHolder authenticatedAccountHolder =
        accountHolderRepository.findAllByAccountId(authenticatedAccount().getId()).get(0);
    return app.bpartners.api.model.BusinessActivity.builder()
        .accountHolder(authenticatedAccountHolder)
        .primaryActivity(rest.getPrimary())
        .secondaryActivity(rest.getSecondary())
        .build();
  }

  public Account authenticatedAccount() {
    return ((Principal) provider.getAuthentication().getPrincipal()).getAccount();
  }
}