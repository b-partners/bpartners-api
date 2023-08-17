package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.OnboardUser;
import app.bpartners.api.endpoint.rest.model.User;
import app.bpartners.api.endpoint.rest.model.UserRole;
import app.bpartners.api.endpoint.rest.security.model.Role;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import static app.bpartners.api.endpoint.rest.security.model.Role.EVAL_PROSPECT;

@Component
@AllArgsConstructor
public class UserRestMapper {
  private final AccountRestMapper accountRestMapper;

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
        .logoFileId(domain.getLogoFileId())
        .activeAccount(accountRestMapper.toRest(domain.getDefaultAccount()))
        .roles(toRest(domain.getRoles()));
  }

  private UserRole toRest(Role role) {
    if (role.getRole().equals(EVAL_PROSPECT.name())) {
      return UserRole.EVAL_PROSPECT;
    }
    return null;
  }

  private List<UserRole> toRest(List<Role> roles) {
    List<UserRole> userRoles = new ArrayList<>();
    roles.forEach(role -> {
      UserRole userRole = toRest(role);
      if (userRole != null) {
        userRoles.add(userRole);
      }
    });
    return userRoles;
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
