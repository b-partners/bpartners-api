package app.bpartners.api.repository;

import app.bpartners.api.model.User;
import app.bpartners.api.model.UserToken;

public interface UserTokenRepository {
  UserToken updateUserToken(User user);

  UserToken getLatestTokenByUser(User user);

  UserToken getLatestTokenByUserId(String userId);

  UserToken getLatestTokenByAccount(String accountId);
}
