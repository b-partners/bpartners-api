package app.bpartners.api.model.mapper;

import app.bpartners.api.model.User;
import app.bpartners.api.model.UserToken;
import app.bpartners.api.repository.bridge.model.User.CreateBridgeUser;
import app.bpartners.api.repository.jpa.model.HUser;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class UserTokenMapper {
  private final UserMapper userMapper;

  public UserToken toDomain(HUser user) {
    return UserToken.builder()
        .user(userMapper.toDomain(user, null))
        .accessToken(user.getAccessToken())
        .expirationDatetime(user.getTokenExpirationDatetime())
        .creationDatetime(user.getTokenCreationDatetime())
        .build();
  }

  public CreateBridgeUser toBridgeAuthUser(User user) {
    return CreateBridgeUser.builder()
        .email(user.getEmail())
        .password(user.getBridgePassword())
        .build();
  }
}
