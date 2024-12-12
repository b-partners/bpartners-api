package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.mapper.UserMapper;
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
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@AllArgsConstructor
public class UserRepositoryImpl implements UserRepository {
  private final UserJpaRepository jpaRepository;
  private final UserMapper userMapper;
  private final CognitoComponent cognitoComponent;
  private final BridgeUserRepository bridgeUserRepository;
  private final AccountHolderJpaRepository holderJpaRepository;
  private final AccountJpaRepository accountJpaRepository;
  private final BankRepository bankRepository;
  private final EntityManager entityManager;

  @Override
  public User getByIdAccount(String idAccount) {
    HAccount account =
        accountJpaRepository
            .findById(idAccount)
            .orElseThrow(() -> new NotFoundException("Account(id=" + idAccount + ") not found"));
    return userMapper.toDomain(account.getUser());
  }

  @Transactional
  @Override
  public List<User> getActiveUsersWithNullSubscription() {
    return jpaRepository.getEnabledUsersWithoutSubscription().stream()
        .map(userMapper::toDomain)
        .toList();
  }

  @Transactional
  @Override
  public List<User> getUsersWithSubscription() {
    return jpaRepository.getUsersWithSubscription().stream().map(userMapper::toDomain).toList();
  }

  @Override
  public List<User> findAll() {
    return jpaRepository.findAll().stream().map(userMapper::toDomain).collect(Collectors.toList());
  }

  @Override
  public Long countUsersByStatus(EnableStatus status) {
    return jpaRepository.countByStatus(status);
  }

  @Override
  public List<User> findAllByCriteria(HashMap<String, Object> criteria) {
    var criteriaBuilder = entityManager.getCriteriaBuilder();
    var query = criteriaBuilder.createQuery(HUser.class);
    var root = query.from(HUser.class);
    var predicates = new ArrayList<Predicate>();
    criteria.forEach(
        (key, value) -> {
          if ("status".equals(key)) {
            var enableStatus = EnableStatus.valueOf(value.toString());
            predicates.add(criteriaBuilder.equal(root.get("status"), enableStatus));
          } else {
            if (!("page".equals(key) || "pageSize".equals(key))) {
              throw new NotImplementedException(
                  "Only status, page and pageSize criteria filter are handled for now but criteria"
                      + " was "
                      + key);
            }
          }
        });
    var page = computeDefaultPage(criteria);
    var pageSize = computeDefaultPageSize(criteria);
    query.where(predicates.toArray(new Predicate[0]));
    return entityManager
        .createQuery(query)
        .setFirstResult(page - 1)
        .setMaxResults(pageSize)
        .getResultList()
        .stream()
        .map(userMapper::toDomain)
        .toList();
  }

  private static Integer computeDefaultPage(HashMap<String, Object> criteria) {
    if (criteria.containsKey("page")) {
      return ((Long) criteria.get("page")).intValue();
    } else {
      return 1;
    }
  }

  private static Integer computeDefaultPageSize(HashMap<String, Object> criteria) {
    if (criteria.containsKey("pageSize")) {
      return ((Long) criteria.get("pageSize")).intValue();
    } else {
      return  500;
    }
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
      entityUser =
          jpaRepository
              .findByEmail(email)
              .orElseThrow(
                  () -> new NotFoundException("No user with the email " + email + " was found"));
    }
    return userMapper.toDomain(entityUser);
  }

  @Override
  public User getByEmail(String email) {
    return userMapper.toDomain(
        jpaRepository
            .findByEmail(email)
            .orElseThrow(
                () -> new NotFoundException("No user with the email " + email + " was found")));
  }

  @Override
  public Optional<User> findByEmail(String email) {
    return jpaRepository.getByEmail(email) != null
        ? Optional.of(userMapper.toDomain(jpaRepository.getByEmail(email)))
        : Optional.empty();
  }

  @Override
  public User getById(String id) {
    return userMapper.toDomain(
        jpaRepository
            .findById(id)
            .orElseThrow(() -> new NotFoundException("User(id=" + id + " not found)")));
  }

  @Override
  public User save(User toSave) {
    List<HAccountHolder> accountHolders = holderJpaRepository.findAllByIdUser(toSave.getId());
    List<HAccount> accounts = accountJpaRepository.findByUser_Id(toSave.getId());
    HUser savedUser = jpaRepository.save(userMapper.toEntity(toSave, accountHolders, accounts));
    Optional<HAccount> optionalAccount =
        accounts.stream().filter(account -> account.getIdBank() != null).findAny();
    String idBank = optionalAccount.isEmpty() ? null : optionalAccount.get().getIdBank();
    return userMapper.toDomain(savedUser, bankRepository.findByExternalId(idBank));
  }

  @Override
  public User create(User user) {
    BridgeUser bridgeUser = bridgeUserRepository.createUser(userMapper.toBridgeUser(user));
    if (bridgeUser == null) {
      BridgeUser.builder().email("it.bpartners@mail.hei.school").build();
    }
    HUser entityToSave = userMapper.toEntity(user, bridgeUser);
    HUser savedUser = jpaRepository.save(entityToSave);
    return userMapper.toDomain(savedUser);
  }

  @Override
  public void deleteById(String id) {
    jpaRepository.deleteById(id);
  }
}
