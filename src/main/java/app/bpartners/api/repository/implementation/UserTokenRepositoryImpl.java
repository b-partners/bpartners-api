package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.User;
import app.bpartners.api.model.UserToken;
import app.bpartners.api.model.mapper.UserMapper;
import app.bpartners.api.model.mapper.UserTokenMapper;
import app.bpartners.api.repository.UserTokenRepository;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.bridge.response.BridgeTokenResponse;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.AccountJpaRepository;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import app.bpartners.api.repository.jpa.model.HAccount;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import app.bpartners.api.repository.jpa.model.HUser;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class UserTokenRepositoryImpl implements UserTokenRepository {
  private final UserJpaRepository userJpaRepository;
  private final UserTokenMapper mapper;
  private final BridgeApi bridgeApi;
  private final UserMapper userMapper;
  private final AccountJpaRepository accountJpaRepository;
  private final AccountHolderJpaRepository holderJpaRepository;

  @Override
  public UserToken updateUserToken(User user) {
    if (user.getEmail() == null || user.getBridgePassword() == null) {
      return null;
    }
    BridgeTokenResponse response = bridgeApi.authenticateUser(mapper.toBridgeAuthUser(user));
    //TODO: do something as retry
    if (response == null
        || !response.getUser().getEmail().equals(user.getEmail())) {
      return null;
    }
    List<HAccount> accounts = accountJpaRepository.findByUser_Id(user.getId());
    List<HAccountHolder> accountHolders = holderJpaRepository.findAllByIdUser(user.getId());
    HUser userEntity = userMapper.toEntity(user, accountHolders, accounts).toBuilder()
        .accessToken(response.getAccessToken())
        .tokenExpirationDatetime(response.getExpirationDate())
        .tokenCreationDatetime(Instant.now())
        .build();
    return mapper.toDomain(userJpaRepository.save(userEntity));
  }

  @Override
  public UserToken getLatestTokenByUser(User user) {
    HUser entity = userJpaRepository.getHUserById(user.getId());
    //TODO: when null or expired then retry 3 times with one second of intervals between
    if (entity.getAccessToken() == null
        || (entity.getTokenExpirationDatetime() != null
        && entity.getTokenExpirationDatetime().isBefore(Instant.now()))) {
      return updateUserToken(mapper.toDomain(entity).getUser());
    }
    //Note that tokens are ordered by expiration datetime desc
    return mapper.toDomain(entity);
  }

  //TODO: check why some accounts are not associated to users
  @Override
  public UserToken getLatestTokenByAccount(String accountId) {
    HUser entity = userJpaRepository.pwGetByAccountId(accountId);
    if (entity.getAccessToken() == null
        || (entity.getTokenExpirationDatetime() != null
        && entity.getTokenExpirationDatetime().isBefore(Instant.now()))) {
      return updateUserToken(mapper.toDomain(entity).getUser());
    }
    //Note that tokens are ordered by expiration datetime desc
    return mapper.toDomain(entity);
  }
}
