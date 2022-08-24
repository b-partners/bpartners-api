package app.bpartners.api.model.mapper;

import app.bpartners.api.model.User;
import app.bpartners.api.model.entity.HUser;
import app.bpartners.api.repository.swan.schema.SwanUser;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class UserMapper {
  private final SwanUserMapper swanUserMapper;

  public User toDomain(HUser entityUser, SwanUser swanUser) {
    return User.builder()
        .id(entityUser.getId())
        .swanUser(swanUserMapper.toDomain(swanUser))
        .monthlySubscription(entityUser.getMonthlySubscription())
        .status(entityUser.getStatus())
        .build();
  }
}
