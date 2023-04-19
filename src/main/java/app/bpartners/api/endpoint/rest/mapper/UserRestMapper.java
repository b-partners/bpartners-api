package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.OnboardUser;
import app.bpartners.api.endpoint.rest.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserRestMapper {
  public User toRest(app.bpartners.api.model.User domain) {
    return new User()
        .id(domain.getId())
        .firstName(domain.getFirstName())
        .lastName(domain.getLastName())
        .phone(domain.getMobilePhoneNumber())
        .monthlySubscriptionAmount(domain.getMonthlySubscription())
        .status(domain.getStatus())
        .idVerified(domain.getIdVerified())
        .identificationStatus(domain.getIdentificationStatus())
        .logoFileId(domain.getLogoFileId());
  }

  public app.bpartners.api.model.User toDomain(OnboardUser toCreateUser) {
    return app.bpartners.api.model.User.builder()
        .firstName(toCreateUser.getFirstName())
        .lastName(toCreateUser.getLastName())
        .email(toCreateUser.getEmail())
        .mobilePhoneNumber(toCreateUser.getPhoneNumber())
        .build();
  }
}
