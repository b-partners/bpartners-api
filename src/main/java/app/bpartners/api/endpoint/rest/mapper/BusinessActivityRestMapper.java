package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.BusinessActivity;
import app.bpartners.api.endpoint.rest.model.CompanyBusinessActivity;
import app.bpartners.api.endpoint.rest.security.AuthenticatedResourceProvider;
import app.bpartners.api.model.AccountHolder;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class BusinessActivityRestMapper {
  private final AuthenticatedResourceProvider authenticatedResourceProvider;

  public BusinessActivity toRest(app.bpartners.api.model.BusinessActivityTemplate domain) {
    return new BusinessActivity()
        .id(domain.getId())
        .name(domain.getName());
  }

  public app.bpartners.api.model.BusinessActivity toDomain(CompanyBusinessActivity rest) {
    AccountHolder authenticatedAccountHolder =
        authenticatedResourceProvider.getAccountHolder();
    return app.bpartners.api.model.BusinessActivity.builder()
        .accountHolder(authenticatedAccountHolder)
        .primaryActivity(rest.getPrimary())
        .secondaryActivity(rest.getSecondary())
        .build();
  }
}