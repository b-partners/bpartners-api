package app.bpartners.api.model.mapper;

import app.bpartners.api.model.User;
import app.bpartners.api.repository.jpa.model.HUser;
import app.bpartners.api.repository.swan.model.SwanUser;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class UserMapper {

  public User toDomain(HUser entityUser, SwanUser swanUser) {
    return User.builder()
        .id(entityUser.getId())
        .firstName(swanUser.firstName)
        .lastName(swanUser.lastName)
        .mobilePhoneNumber(swanUser.mobilePhoneNumber)
        .monthlySubscription(entityUser.getMonthlySubscription())
        .status(entityUser.getStatus())
        .build();
  }
}
