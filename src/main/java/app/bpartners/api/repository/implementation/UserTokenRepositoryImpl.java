package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.User;
import app.bpartners.api.model.UserToken;
import app.bpartners.api.model.mapper.UserTokenMapper;
import app.bpartners.api.repository.UserTokenRepository;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.bridge.response.BridgeTokenResponse;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import app.bpartners.api.repository.jpa.model.HUser;
import java.time.Instant;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class UserTokenRepositoryImpl implements UserTokenRepository {
  private final UserJpaRepository userJpaRepository;
  private final UserTokenMapper mapper;
  private final BridgeApi bridgeApi;

  @Override
  public UserToken updateUserToken(User user) {
    BridgeTokenResponse response = bridgeApi.authenticateUser(mapper.toBridgeAuthUser(user));
    //TODO: do something as retry
    if (response == null
        || !response.getUser().getEmail().equals(user.getEmail())) {
      return null;
    }
    HUser entity = userJpaRepository.getById(user.getId());
    return mapper.toDomain(userJpaRepository.save(entity.toBuilder()
        .accessToken(response.getAccessToken())
        .tokenExpirationDatetime(response.getExpirationDate())
        .tokenCreationDatetime(Instant.now())
        .build()));
  }

  @Override
  public UserToken getLatestTokenByUser(User user) {
    HUser entity = userJpaRepository.getById(user.getId());
    //TODO: when null then retry 3 times with one second of intervals between
    if (entity.getAccessToken() == null) {
      return updateUserToken(user);
    }
    //Note that tokens are ordered by expiration datetime desc
    return mapper.toDomain(entity);
  }
}
