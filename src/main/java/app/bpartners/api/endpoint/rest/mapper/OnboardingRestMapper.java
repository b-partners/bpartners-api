package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.OnboardUser;
import app.bpartners.api.endpoint.rest.model.OnboardedUser;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class OnboardingRestMapper {
  private final UserRestMapper userMapper;
  private final AccountRestMapper accountMapper;
  private final AccountHolderRestMapper holderMapper;

  public OnboardedUser toRest(app.bpartners.api.model.OnboardedUser domain) {
    return new OnboardedUser()
        .user(userMapper.toRest(domain.getOnboardedUser()))
        .account(accountMapper.toRest(domain.getOnboardedAccount()))
        .accountHolder(holderMapper.toRest(domain.getOnboardedAccountHolder()));
  }

  public app.bpartners.api.model.OnboardUser toDomain(OnboardUser rest) {
    return app.bpartners.api.model.OnboardUser.builder()
        .user(userMapper.toDomain(rest))
        .companyName(rest.getCompanyName())
        .build();
  }
}
