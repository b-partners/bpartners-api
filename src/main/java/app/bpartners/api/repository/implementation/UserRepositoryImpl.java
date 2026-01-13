package app.bpartners.api.repository.implementation;

import static java.util.stream.Collectors.toList;

import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.mapper.UserMapper;
import app.bpartners.api.repository.BankRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.bridge.model.User.BridgeUser;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.AccountJpaRepository;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import app.bpartners.api.repository.jpa.model.HAccount;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import app.bpartners.api.repository.jpa.model.HUser;
import app.bpartners.api.service.subscription.StripePaymentMethodService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Repository
@AllArgsConstructor
public class UserRepositoryImpl implements UserRepository {
  private final UserJpaRepository jpaRepository;
  private final UserMapper userMapper;
  private final CognitoComponent cognitoComponent;
  private final AccountHolderJpaRepository holderJpaRepository;
  private final AccountJpaRepository accountJpaRepository;
  private final BankRepository bankRepository;
  private final EntityManager entityManager;
  private final StripePaymentMethodService stripePaymentMethodService;

  @Override
  public User getByIdAccount(String idAccount) {
    HAccount account =
        accountJpaRepository
            .findById(idAccount)
            .orElseThrow(() -> new NotFoundException("Account(id=" + idAccount + ") not found"));
    var fetchedUser = userMapper.toDomain(account.getUser());
    return retrievePaymentMethod(fetchedUser);
  }

  @Transactional
  @Override
  public List<User> getActiveUsersWithNullSubscription() {
    return jpaRepository.getEnabledUsersWithoutSubscription().stream()
        .map(
            userEntity -> {
              var fetchedUser = userMapper.toDomain(userEntity);
              return retrievePaymentMethod(fetchedUser);
            })
        .toList();
  }

  @Transactional
  @Override
  public List<User> getUsersWithSubscription() {
    return jpaRepository.getUsersWithSubscription().stream()
        .map(
            userEntity -> {
              var fetchedUser = userMapper.toDomain(userEntity);
              return retrievePaymentMethod(fetchedUser);
            })
        .toList();
  }

  @Override
  public List<User> findSubordinatesUsersByParentId(String parentId) {
    return jpaRepository.findSubordinatesUsersByParentId(parentId).stream()
        .map(
            userEntity -> {
              var fetchedUser = userMapper.toDomain(userEntity);
              return retrievePaymentMethod(fetchedUser);
            })
        .toList();
  }

  @Override
  public List<User> findAll() {
    return jpaRepository.findAll().stream()
        .map(
            userEntity -> {
              var fetchedUser = userMapper.toDomain(userEntity);
              return retrievePaymentMethod(fetchedUser);
            })
        .collect(toList());
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
          } else if ("email".equals(key)) {
            predicates.add(criteriaBuilder.like(root.get("email"), "%" + value.toString() + "%"));
          } else {
            if (!("page".equals(key) || "pageSize".equals(key))) {
              throw new NotImplementedException(
                  "Only email, status, page and pageSize criteria filter are handled for now but"
                      + " criteria was "
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
        .map(
            userEntity -> {
              var fetchedUser = userMapper.toDomain(userEntity);
              return retrievePaymentMethod(fetchedUser);
            })
        .toList();
  }

  private static Integer computeDefaultPage(HashMap<String, Object> criteria) {
    if (criteria.containsKey("page")) {
      Object pageObject = criteria.get("page");
      if (pageObject instanceof Integer) {
        return (Integer) pageObject;
      }
      if (pageObject instanceof Long) {
        return ((Long) pageObject).intValue();
      }
      return 1;
    }
    return 1;
  }

  private static Integer computeDefaultPageSize(HashMap<String, Object> criteria) {
    if (criteria.containsKey("pageSize")) {
      Object pageSizeObject = criteria.get("pageSize");
      if (pageSizeObject instanceof Integer) {
        return (Integer) pageSizeObject;
      }
      if (pageSizeObject instanceof Long) {
        return ((Long) pageSizeObject).intValue();
      }
      return 500;
    }
    return 500;
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
    var fetchedUser = userMapper.toDomain(entityUser);
    return retrievePaymentMethod(fetchedUser);
  }

  @Override
  public User getUserByApiKey(String apiKey) {
    var user =
        jpaRepository
            .findByApiKey(apiKey)
            .orElseThrow(
                () -> new NotFoundException("No user with the apiKey " + apiKey + " was found"));
    var fetchedUser = userMapper.toDomain(user);
    return retrievePaymentMethod(fetchedUser);
  }

  @Override
  public User getByEmail(String email) {
    var fetchedUser =
        userMapper.toDomain(
            jpaRepository
                .findByEmail(email)
                .orElseThrow(
                    () -> new NotFoundException("No user with the email " + email + " was found")));
    return retrievePaymentMethod(fetchedUser);
  }

  @Override
  public Optional<User> findByEmail(String email) {
    var mail = jpaRepository.findByEmail(email);
    return jpaRepository.getByEmail(email) != null
        ? Optional.of(retrievePaymentMethod(userMapper.toDomain(jpaRepository.getByEmail(email))))
        : Optional.empty();
  }

  @Override
  public User getById(String id) {
    var fetchedUser =
        userMapper.toDomain(
            jpaRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("User(id=" + id + " not found)")));
    return retrievePaymentMethod(fetchedUser);
  }

  @Override
  public User save(User toSave) {
    List<HAccountHolder> accountHolders = holderJpaRepository.findAllByIdUser(toSave.getId());
    List<HAccount> accounts = accountJpaRepository.findByUser_Id(toSave.getId());
    HUser savedUser = jpaRepository.save(userMapper.toEntity(toSave, accountHolders, accounts));
    var fetchedUser = userMapper.toDomain(savedUser);
    return retrievePaymentMethod(fetchedUser);
  }

  @Override
  public User create(User user) {
    BridgeUser bridgeUser = BridgeUser.builder().email(user.getEmail()).build();
    HUser entityToSave = userMapper.toEntity(user, bridgeUser);
    HUser savedUser = jpaRepository.save(entityToSave);
    var fetchedUser = userMapper.toDomain(savedUser);
    return retrievePaymentMethod(fetchedUser);
  }

  private User retrievePaymentMethod(User fetchedUser) {
    if (fetchedUser == null) {
      return null;
    }
    if (fetchedUser.getUserSubscriptionId() != null) {
      var paymentMethodList =
          stripePaymentMethodService.getPaymentMethod(fetchedUser.getUserSubscriptionId());
      return fetchedUser.toBuilder().paymentMethodExists(!paymentMethodList.isEmpty()).build();
    }
    return fetchedUser;
  }

  @Override
  public void deleteById(String id) {
    jpaRepository.deleteById(id);
  }
}
