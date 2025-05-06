package app.bpartners.api.service.user;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.UserRegistrationRequested;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserToken;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.UserTokenRepository;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.AccountJpaRepository;
import app.bpartners.api.repository.jpa.InvoiceSummaryJpaRepository;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import app.bpartners.api.repository.jpa.model.HUser;
import app.bpartners.api.service.SnsService;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class UserService {
  private final UserRepository userRepository;
  private final UserTokenRepository userTokenRepository;
  private final SnsService snsService;
  private final CognitoComponent cognitoComponent;
  private final UserJpaRepository userJpaRepository;
  private final AccountJpaRepository accountJpaRepository;
  private final AccountHolderJpaRepository accountHolderJpaRepository;
  private final InvoiceSummaryJpaRepository invoiceSummaryJpaRepository;
  private final BridgeApi bridgeApi;
  private final EventProducer<UserRegistrationRequested> eventProducer;

  @Transactional
  public User getByIdAccount(String idAccount) {
    return userRepository.getByIdAccount(idAccount);
  }

  @Transactional
  public List<User> findSubordinatesUsersByParentId(String parentId) {
    return userRepository.findSubordinatesUsersByParentId(parentId);
  }

  @Transactional
  public User registerDevice(String idUser, String token) {
    User user = userRepository.getById(idUser);
    String actualToken = user.getDeviceToken();
    if (actualToken != null && actualToken.equals(token)) {
      return user;
    }
    String actualArn = user.getSnsArn();
    if (actualArn != null) {
      snsService.deleteEndpointArn(actualArn);
    }
    String snsArn = snsService.createEndpointArn(token);
    return save(user.toBuilder().snsArn(snsArn).deviceToken(token).build());
  }

  @Transactional
  public User changeActiveAccount(String idUser, String idAccount) {
    User user = userRepository.getById(idUser);
    if (user.getDefaultAccount().getId().equals(idAccount)) {
      return user;
    }
    boolean accountIsAssociated =
        user.getAccounts().stream().anyMatch(account -> account.getId().equals(idAccount));
    if (!accountIsAssociated) {
      throw new NotFoundException(
          "Account(id=" + idAccount + ") is not found for User(id=" + idUser + ")");
    }

    return userRepository.save(user.toBuilder().preferredAccountId(idAccount).build());
  }

  @Transactional
  public User save(User toSave) {
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

  @Transactional
  public void deleteUserByEmail(String email) {
    HUser user = userJpaRepository.getByEmail(email);
    if (user == null) {
      throw new NotFoundException(String.format("The user %s is not found", email));
    }
    bridgeApi.deleteUser(user.getBridgeUserId(), user.getBridgePassword());
    invoiceSummaryJpaRepository.deleteByIdUser(user.getId());
    accountJpaRepository.deleteHAccountByUserId(user.getId());
    accountHolderJpaRepository.deleteByIdUser(user.getBridgeUserId());
    userRepository.deleteById(user.getId());
    cognitoComponent.deleteUserByUsername(email);
  }

  @Transactional
  public List<User> registerOnStripeActiveUsersWithNullSubscription() {
    List<User> users = userRepository.getActiveUsersWithNullSubscription();
    var totalUser = users.size();
    int[] userNb = {0};
    users.forEach(
        user -> {
          eventProducer.accept(
              List.of(
                  UserRegistrationRequested.builder()
                      .userId(user.getId())
                      .totalNbUser(totalUser)
                      .userNb(userNb[0])
                      .build()));
          userNb[0]++;
        });
    // TODO: replace to users count who requested registration not those with active subscriptions
    return userRepository.getUsersWithSubscription();
  }

  public User getUserByApiKey(String apikey) {
    return userRepository.getUserByApiKey(apikey);
  }
}
