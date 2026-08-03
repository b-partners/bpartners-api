package app.bpartners.api.repository.implementation;

import static java.util.stream.Collectors.toList;

import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.mapper.UserMapper;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.AccountJpaRepository;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import app.bpartners.api.repository.jpa.model.HAccount;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import app.bpartners.api.repository.jpa.model.HUser;
import app.bpartners.api.service.subscription.StripePaymentMethodService;
import com.stripe.exception.StripeException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import java.util.*;
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
  private final AccountHolderJpaRepository holderJpaRepository;
  private final AccountJpaRepository accountJpaRepository;
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
    return jpaRepository.findAll().stream().map(userMapper::toDomain).collect(toList());
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
        .setFirstResult((page - 1) * pageSize)
        .setMaxResults(pageSize)
        .getResultList()
        .stream()
        .map(userMapper::toDomain)
        .toList();
  }

  @Override
  public List<String> findEnabledUserIdsWithSubscription() {
    // Projection on id only: avoids hydrating the whole HUser graph (accounts, holders, banks,
    // subscription products, parent user...) which triggers a heavy N+1 over the full user base.
    return entityManager
        .createQuery(
            "SELECT u.id FROM HUser u "
                + "WHERE u.status = :status AND u.userSubscriptionE2Id IS NOT NULL",
            String.class)
        .setParameter("status", EnableStatus.ENABLED)
        .getResultList();
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
  public Optional<User> findByApiKey(String apiKey) {
    var user = jpaRepository.findByApiKey(apiKey).orElse(null);
    if (user == null) {
      return Optional.empty();
    }
    var fetchedUser = userMapper.toDomain(user);
    return Optional.of(retrievePaymentMethod(fetchedUser));
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

  @Transactional
  @Override
  public Optional<User> findByStripeCustomerId(String stripeCustomerId) {
    return jpaRepository
        .findByUserSubscriptionE2Id(stripeCustomerId)
        .map(userMapper::toDomain)
        .map(this::retrievePaymentMethod);
  }

  @Override
  public User getById(String id) {
    return retrievePaymentMethod(getByIdWithoutPaymentMethod(id));
  }

  @Override
  public User getByIdWithoutPaymentMethod(String id) {
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
    var fetchedUser = userMapper.toDomain(savedUser);
    return retrievePaymentMethod(fetchedUser);
  }

  @Override
  public User create(User user) {
    HUser entityToSave = userMapper.toEntity(user);
    HUser savedUser = jpaRepository.save(entityToSave);
    var fetchedUser = userMapper.toDomain(savedUser);
    return retrievePaymentMethod(fetchedUser);
  }

  @Override
  public User retrievePaymentMethod(User fetchedUser) {
    if (fetchedUser == null) {
      return null;
    }
    if (fetchedUser.getUserSubscriptionId() != null) {
      try {
        var stripeCustomerIdentifier = fetchedUser.getUserSubscriptionId();
        var paymentMethodList =
            stripePaymentMethodService.getPaymentMethod(stripeCustomerIdentifier);
        return fetchedUser.toBuilder()
            .paymentMethodExists(
                !paymentMethodList.isEmpty()
                    && paymentMethodList.stream()
                        .anyMatch(StripePaymentMethodService::isPaymentMethodValid))
            .build();
      } catch (StripeException e) {
        log.error(
            "Unable to retrieve user(id={}) payment method due to stripe exception {}",
            fetchedUser.getId(),
            e.getMessage());
        return fetchedUser;
      }
    }
    return fetchedUser;
  }

  @Override
  public void deleteById(String id) {
    jpaRepository.deleteById(id);
  }
}
