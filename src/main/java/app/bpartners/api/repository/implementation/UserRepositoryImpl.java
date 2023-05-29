package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.UserMapper;
import app.bpartners.api.repository.AccountRepository;
import app.bpartners.api.repository.BankRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.bridge.model.User.BridgeUser;
import app.bpartners.api.repository.bridge.repository.BridgeUserRepository;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.AccountJpaRepository;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import app.bpartners.api.repository.jpa.model.HAccount;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import app.bpartners.api.repository.jpa.model.HUser;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class UserRepositoryImpl implements UserRepository {
  private final UserJpaRepository jpaRepository;
  private final UserMapper userMapper;
  private final CognitoComponent cognitoComponent;
  private final BridgeUserRepository bridgeUserRepository;
  private final AccountHolderJpaRepository holderJpaRepository;
  private final AccountJpaRepository accountJpaRepository;
  private final AccountRepository accountRepository;
  private final BankRepository bankRepository;

  @Override
  public List<User> findAll() {
    return jpaRepository.findAll().stream()
        .map(userMapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<User> findAllWithUpdatedAccounts() {
    return jpaRepository.findAll().stream()
        .map(user -> userMapper.toDomain(user, accountRepository.findByUserId(user.getId())))
        .collect(Collectors.toList());
  }

  @Override
  public User getUserByToken(String token) {
    HUser entityUser;
    Optional<HUser> entitiesFromBridge = jpaRepository.findByAccessToken(token);
    if (entitiesFromBridge.isPresent()) {
      entityUser = entitiesFromBridge.get();
    } else {
      String cognitoToken = token;
      String email = cognitoComponent.getEmailByToken(cognitoToken);
      entityUser = jpaRepository.findByEmail(email).orElseThrow(
          () -> new NotFoundException(
              "No user with the email " + email + " was found"));
    }
    return userMapper.toDomain(entityUser);
  }

  @Override
  public User getByEmail(String email) {
    return userMapper.toDomain(
        jpaRepository.findByEmail(email).orElseThrow(
            () -> new NotFoundException(
                "No user with the email " + email + " was found")));
  }

  @Override
  public Optional<User> findByEmail(String email) {
    return jpaRepository.getByEmail(email) != null
        ? Optional.of(userMapper.toDomain(jpaRepository.getByEmail(email)))
        : Optional.empty();
  }

  @Override
  public User getById(String id) {
    return userMapper.toDomain(jpaRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("User(id=" + id + " not found)")));
  }

  @Override
  public User save(User toSave) {
    List<HAccountHolder> accountHolders =
        holderJpaRepository.findAllByIdUser(toSave.getId());
    List<HAccount> accounts =
        accountJpaRepository.findByUser_Id(toSave.getId());
    HUser savedUser = jpaRepository.save(
        userMapper.toEntity(toSave, accountHolders, accounts));
    Optional<HAccount> optionalAccount = accounts.stream()
        .filter(account -> account.getIdBank() != null)
        .findAny();
    String idBank = optionalAccount.isEmpty() ? null
        : optionalAccount.get().getIdBank();
    return userMapper.toDomain(savedUser, bankRepository.findByExternalId(idBank));
  }

  @Override
  public User create(User user) {
    BridgeUser bridgeUser = bridgeUserRepository.createUser(userMapper.toBridgeUser(user));
    HUser entityToSave = userMapper.toEntity(user, bridgeUser);
    HUser savedUser = jpaRepository.save(entityToSave);
    return userMapper.toDomain(savedUser);
  }

  @Override
  public User getByBearer(String bearer) {
    return userMapper.toDomain(jpaRepository.getByAccessToken(bearer).orElseThrow(
        () -> new NotFoundException("Any user were found associated to bearer. " + bearer)));
  }
}
