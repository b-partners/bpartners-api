package app.bpartners.api.repository;

import app.bpartners.api.model.User;
import app.bpartners.api.model.UserToken;
import app.bpartners.api.repository.jpa.model.HUser;
import java.time.Instant;

public interface UserTokenRepository {
  UserToken updateUserToken(User user);

  UserToken getLatestTokenByUser(User user);

  UserToken getLatestTokenByAccount(String accountId);
}
