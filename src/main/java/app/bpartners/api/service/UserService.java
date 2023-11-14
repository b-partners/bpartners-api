package app.bpartners.api.service;

import app.bpartners.api.model.User;
import app.bpartners.api.model.UserToken;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.UserTokenRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@AllArgsConstructor
public class UserService {
  private final UserRepository userRepository;
  private final UserTokenRepository userTokenRepository;
  private final SnsService snsService;

  @Transactional
  public User registerDevice(String idUser, String token) {
    User user = userRepository.getById(idUser);
    if (user.getSnsArn() != null) {
      throw new BadRequestException("User(id=" + idUser + ") has already a SNS endpoint ARN");
    }
    String snsArn = snsService.createEndpointArn(token);
    return saveUser(user.toBuilder()
        .snsArn(snsArn)
        .build());
  }

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

  @Transactional
  public User saveUser(User toSave) {
    return userRepository.save(toSave);
  }

  @Transactional
  public User getUserById(String id) {
    return userRepository.getById(id);
  }

  @Transactional
  public User getUserByEmail(String email) {
    return userRepository.getByEmail(email);
  }

  @Transactional
  public User getUserByToken(String token) {
    return userRepository.getUserByToken(token);
  }

  @Transactional
  public UserToken getLatestToken(User user) {
    return userTokenRepository.getLatestTokenByUser(user);
  }

  @Transactional
  public List<User> findAll() {
    return userRepository.findAll();
  }

  @Transactional
  public UserToken getLatestTokenByAccount(String accountId) {
    return userTokenRepository.getLatestTokenByAccount(accountId);
  }
}
