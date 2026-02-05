package app.bpartners.api.service.user;

import static app.bpartners.api.endpoint.rest.model.UserApiKeyType.ANALYSIS;
import static app.bpartners.api.endpoint.rest.model.UserApiKeyType.DASHBOARD;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.UserRegistrationRequested;
import app.bpartners.api.endpoint.rest.model.UserApiKey;
import app.bpartners.api.endpoint.rest.model.UserApiKeyType;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.UserApiKeyMapper;
import app.bpartners.api.repository.UserAnalysisApiKeyRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.AccountJpaRepository;
import app.bpartners.api.repository.jpa.InvoiceSummaryJpaRepository;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import app.bpartners.api.repository.jpa.model.HUser;
import app.bpartners.api.service.SnsService;
import java.util.HashMap;
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
  private final SnsService snsService;
  private final CognitoComponent cognitoComponent;
  private final UserJpaRepository userJpaRepository;
  private final AccountJpaRepository accountJpaRepository;
  private final AccountHolderJpaRepository accountHolderJpaRepository;
  private final InvoiceSummaryJpaRepository invoiceSummaryJpaRepository;
  private final EventProducer<UserRegistrationRequested> eventProducer;
  private final UserAnalysisApiKeyRepository analysisApiKeyRepository;
  private final UserApiKeyMapper userApiKeyMapper;

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
  public List<User> findAll() {
    return userRepository.findAll();
  }

  public List<User> getUsersByCriteria(HashMap<String, Object> criteria) {
    return userRepository.findAllByCriteria(criteria);
  }

  @Transactional
  public void deleteUserByEmail(String email) {
    HUser user = userJpaRepository.getByEmail(email);
    if (user == null) {
      throw new NotFoundException(String.format("The user %s is not found", email));
    }
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

  // TODO : delete and replace test with getApiKeys method
  public List<UserAnalysisApiKey> getAnalysisApiKeys(String userId) {
    return analysisApiKeyRepository.getAllByUserId(userId);
  }

  public List<UserApiKey> getApiKeys(String userId, List<UserApiKeyType> keyTypes) {
    User user = userRepository.getById(userId);
    return getApiKeys(user, keyTypes);
  }

  // TODO: create (if not exists) internal UserApiKey component
  public List<UserApiKey> getApiKeys(User user, List<UserApiKeyType> keyTypes) {
    List<UserApiKeyType> types;
    if (keyTypes == null || keyTypes.isEmpty()) {
      types = List.of(DASHBOARD);
    } else {
      types = keyTypes;
    }
    var userId = user.getId();
    return types.stream()
        .map(
            userApiKeyType -> {
              if (DASHBOARD.equals(userApiKeyType)) {
                return List.of(
                    new UserApiKey().key(user.getApiKey()).type(DASHBOARD).enabled(true));
              }
              if (ANALYSIS.equals(userApiKeyType)) {
                return analysisApiKeyRepository.getAllByUserId(userId).stream()
                    .map(userApiKeyMapper::toRest)
                    .toList();
              }
              throw new UnsupportedOperationException("Unexpected key type: " + userApiKeyType);
            })
        .flatMap(List::stream)
        .toList();
  }
}
