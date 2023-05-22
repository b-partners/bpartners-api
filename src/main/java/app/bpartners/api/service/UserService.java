package app.bpartners.api.service;

import app.bpartners.api.model.User;
import app.bpartners.api.model.UserToken;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.UserTokenRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.transaction.annotation.Isolation.SERIALIZABLE;


@Service
@AllArgsConstructor
public class UserService {
  private final UserRepository userRepository;
  private final UserTokenRepository userTokenRepository;

  @Transactional
  public User changeActiveAccount(String idUser, String idAccount) {
    User user = userRepository.getById(idUser);
    if (user.getDefaultAccount().getId().equals(idAccount)) {
      return user;
    }
    boolean accountIsAssociated = user.getAccounts().stream()
        .anyMatch(account -> account.getId().equals(idAccount));
    if (!accountIsAssociated) {
      throw new NotFoundException(
          "Account(id=" + idAccount + ") is not found for User(id=" + idUser + ")");
    }

    return userRepository.save(user.toBuilder()
        .preferredAccountId(idAccount)
        .build());
  }

  @Transactional(isolation = SERIALIZABLE)
  public User getUserByEmail(String email) {
    return userRepository.getByEmail(email);
  }

  @Transactional(isolation = SERIALIZABLE)
  public User getUserByIdAndBearer(String swanUserId, String accessToken) {
    return userRepository.getUserBySwanUserIdAndToken(swanUserId, accessToken);
  }

  @Transactional(isolation = SERIALIZABLE)
  public User getUserByToken(String token) {
    return userRepository.getUserByToken(token);
  }

  @Transactional(isolation = SERIALIZABLE)
  public UserToken getLatestToken(User user) {
    return userTokenRepository.getLatestTokenByUser(user);
  }

  @Transactional(isolation = SERIALIZABLE)
  public List<User> findAll() {
    return userRepository.findAll();
  }

  @Transactional(isolation = SERIALIZABLE)
  public UserToken getLatestTokenByAccount(String accountId) {
    return userTokenRepository.getLatestTokenByAccount(accountId);
  }
}
