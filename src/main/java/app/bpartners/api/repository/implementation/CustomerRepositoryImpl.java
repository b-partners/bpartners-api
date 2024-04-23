package app.bpartners.api.repository.implementation;

import static app.bpartners.api.repository.jpa.model.HCustomer.UPDATED_AT_PROPERTY;

import app.bpartners.api.endpoint.rest.model.CustomerStatus;
import app.bpartners.api.endpoint.rest.model.CustomerType;
import app.bpartners.api.endpoint.rest.model.UpdateCustomerStatus;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.CustomerMapper;
import app.bpartners.api.repository.CustomerRepository;
import app.bpartners.api.repository.jpa.CustomerJpaRepository;
import app.bpartners.api.repository.jpa.model.HCustomer;
import app.bpartners.api.repository.jpa.model.HHasCustomer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
@Slf4j
public class CustomerRepositoryImpl implements CustomerRepository {
  private static final String FIRST_NAME = "firstName";
  private static final String LAST_NAME = "lastName";
  private static final String EMAIL = "email";
  private static final String PHONE = "phone";
  private static final String COUNTRY = "country";
  private static final String CITY = "city";
  private static final String STATUS = "status";
  private final CustomerJpaRepository jpaRepository;
  private final CustomerMapper mapper;
  private final EntityManager entityManager;

  private static void warnDeprecated(String field) {
    log.warn(
        "DEPRECATED: query parameter {} is still used for filtering customers."
            + " Use the query parameter filters instead.",
        field);
  }

  private static Predicate[] retrieveNotNullPredicates(
      String idUser,
      String firstname,
      String lastname,
      String email,
      String phoneNumber,
      String city,
      String country,
      CustomerStatus status,
      String prospectId,
      CriteriaBuilder builder,
      Root<HCustomer> root,
      List<Predicate> predicates) {
    predicates.add(builder.equal(root.get("idUser"), idUser));
    if (firstname != null) {
      warnDeprecated(FIRST_NAME);
      predicates.add(
          builder.or(
              builder.like(root.get(FIRST_NAME), "%" + firstname.toLowerCase() + "%"),
              builder.like(
                  builder.lower(root.get(FIRST_NAME)), "%" + firstname.toLowerCase() + "%")));
    }
    if (lastname != null) {
      warnDeprecated(LAST_NAME);
      predicates.add(
          builder.or(
              builder.like(builder.lower(root.get(LAST_NAME)), "%" + lastname.toLowerCase() + "%"),
              builder.like(root.get(LAST_NAME), "%" + lastname.toLowerCase() + "%")));
    }

    if (email != null) {
      warnDeprecated(EMAIL);
      predicates.add(
          builder.or(
              builder.like(builder.lower(root.get(EMAIL)), "%" + email.toLowerCase() + "%"),
              builder.like(root.get(EMAIL), "%" + email.toLowerCase() + "%")));
    }
    if (phoneNumber != null) {
      warnDeprecated("phoneNumber");
      predicates.add(
          builder.or(
              builder.like(builder.lower(root.get(PHONE)), "%" + phoneNumber.toLowerCase() + "%"),
              builder.like(root.get(PHONE), "%" + phoneNumber.toLowerCase() + "%")));
    }
    if (city != null) {
      warnDeprecated(CITY);
      predicates.add(
          builder.or(
              builder.like(builder.lower(root.get(CITY)), "%" + city.toLowerCase() + "%"),
              builder.like(root.get(CITY), "%" + city.toLowerCase() + "%")));
    }
    if (country != null) {
      warnDeprecated(COUNTRY);
      predicates.add(
          builder.or(
              builder.like(builder.lower(root.get(COUNTRY)), "%" + country.toLowerCase() + "%"),
              builder.like(root.get(COUNTRY), "%" + country.toLowerCase() + "%")));
    }
    if (prospectId != null) {
      Join<HCustomer, HHasCustomer> customerProspectJoin = root.join("prospect");
      predicates.add(builder.equal(customerProspectJoin.get("id"), prospectId));
    }
    if (status != null) {
      predicates.add(builder.equal(root.get(STATUS), status));
    } else {
      predicates.add(builder.equal(root.get(STATUS), CustomerStatus.ENABLED));
    }
    return predicates.toArray(new Predicate[0]);
  }

  private static List<Customer> filterBy(
      String idUser,
      List<String> keywords,
      CustomerStatus status,
      String prospectId,
      Pageable pageable,
      EntityManager entityManager,
      CustomerMapper mapper) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<HCustomer> query = builder.createQuery(HCustomer.class);
    List<Predicate> predicates = new ArrayList<>();
    Root<HCustomer> root = query.from(HCustomer.class);
    predicates.add(builder.equal(root.get("idUser"), idUser));
    predicates.add(
        status != null
            ? builder.equal(root.get(STATUS), status)
            : builder.equal(root.get(STATUS), CustomerStatus.ENABLED));
    if (prospectId != null) {
      Join<HCustomer, HHasCustomer> customerProspectJoin = root.join("prospect");
      predicates.add(builder.equal(customerProspectJoin.get("id"), prospectId));
    }
    List<Predicate> keywordsPredicates = new ArrayList<>();
    for (String keyword : keywords) {
      keywordsPredicates.add(
          builder.like(builder.lower(root.get(FIRST_NAME)), "%" + keyword + "%"));
      keywordsPredicates.add(
          builder.like(builder.lower(root.get(FIRST_NAME)), "%" + keyword + "%"));
      keywordsPredicates.add(builder.like(builder.lower(root.get(LAST_NAME)), "%" + keyword + "%"));
      keywordsPredicates.add(builder.like(builder.lower(root.get(EMAIL)), "%" + keyword + "%"));
      keywordsPredicates.add(builder.like(builder.lower(root.get(PHONE)), "%" + keyword + "%"));
      keywordsPredicates.add(builder.like(builder.lower(root.get(CITY)), "%" + keyword + "%"));
      keywordsPredicates.add(builder.like(builder.lower(root.get(COUNTRY)), "%" + keyword + "%"));
    }
    predicates.add(builder.or(keywordsPredicates.toArray(new Predicate[0])));
    query
        .where(builder.and(predicates.toArray(new Predicate[0])))
        .orderBy(QueryUtils.toOrders(updatedAtDescSort(), root, builder));
    return entityManager
        .createQuery(query)
        .setFirstResult((pageable.getPageNumber()) * pageable.getPageSize())
        .setMaxResults(pageable.getPageSize())
        .getResultList()
        .stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  private static List<Customer> filterBy(
      String idUser,
      String firstName,
      String lastName,
      String email,
      String phoneNumber,
      String city,
      String country,
      CustomerStatus status,
      String prospectId,
      Pageable pageable,
      EntityManager entityManager,
      CustomerMapper mapper) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<HCustomer> query = builder.createQuery(HCustomer.class);
    List<Predicate> predicates = new ArrayList<>();
    Root<HCustomer> root = query.from(HCustomer.class);
    Predicate[] arrays =
        retrieveNotNullPredicates(
            idUser,
            firstName,
            lastName,
            email,
            phoneNumber,
            city,
            country,
            status,
            prospectId,
            builder,
            root,
            predicates);
    query
        .where(builder.and(predicates.toArray(arrays)))
        .orderBy(QueryUtils.toOrders(updatedAtDescSort(), root, builder));
    return entityManager
        .createQuery(query)
        .setFirstResult((pageable.getPageNumber()) * pageable.getPageSize())
        .setMaxResults(pageable.getPageSize())
        .getResultList()
        .stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<Customer> findByIdUserAndCriteria(
      String idUser,
      String firstName,
      String lastName,
      String email,
      String phoneNumber,
      String city,
      String country,
      List<String> keywords,
      String prospectId,
      CustomerStatus status,
      int page,
      int pageSize) {
    Pageable pageable = PageRequest.of(page, pageSize);
    if (keywords != null && !keywords.isEmpty()) {
      return filterBy(idUser, keywords, status, prospectId, pageable, entityManager, mapper);
    } else {
      return filterBy(
          idUser,
          firstName,
          lastName,
          email,
          phoneNumber,
          city,
          country,
          status,
          prospectId,
          pageable,
          entityManager,
          mapper);
    }
  }

  @Override
  public List<Customer> updateCustomersStatuses(List<UpdateCustomerStatus> customerStatusList) {
    List<HCustomer> customersToUpdate =
        customerStatusList.stream()
            .map(
                customerStatus ->
                    jpaRepository
                        .findById(customerStatus.getId())
                        .orElseThrow(
                            () ->
                                new NotFoundException(
                                    "Customer(id=" + customerStatus.getId() + " not found"))
                        .toBuilder()
                        .status(customerStatus.getStatus())
                        .build())
            .collect(Collectors.toList());
    return jpaRepository.saveAll(customersToUpdate).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<Customer> findWhereLatitudeOrLongitudeIsNull() {
    return jpaRepository.findAllByLatitudeIsNullOrLongitudeIsNull().stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<Customer> findAllByIdUserOrderByLastNameAsc(String idUser) {
    return jpaRepository.findAllByIdUserOrderByLastNameAsc(idUser).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<Customer> findByIdAccountHolder(String idAccountHolder) {
    if (idAccountHolder == null) {
      return List.of();
    }
    return jpaRepository.findAllByIdAccountHolder(idAccountHolder).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<Customer> saveAll(List<Customer> toCreate) {
    List<HCustomer> toSave =
        toCreate.stream().map(this::checkExisting).map(mapper::toEntity).toList();

    List<HCustomer> entitySaved = jpaRepository.saveAll(toSave);

    checkRecentlyAdded(toSave, entitySaved);

    return entitySaved.stream().map(mapper::toDomain).toList();
  }

  @Override
  public Customer save(Customer toSave) {
    return mapper.toDomain(jpaRepository.save(mapper.toEntity(toSave)));
  }

  private void checkRecentlyAdded(List<HCustomer> toSave, List<HCustomer> saved) {
    for (HCustomer c1 : saved) {
      for (HCustomer c2 : toSave) {
        if (c1.getId().equals(c2.getId())) {
          c1.setRecentlyAdded(c2.isRecentlyAdded());
          break;
        }
      }
    }
  }

  @Override
  public Customer findById(String id) {
    return mapper.toDomain(
        jpaRepository
            .findById(id)
            .orElseThrow(() -> new NotFoundException("Customer." + id + " is not found.")));
  }

  @Override
  public Optional<Customer> findOptionalByProspectId(String idProspect) {
    return jpaRepository.findByIdProspect(idProspect).map(mapper::toDomain);
  }

  @Override
  public Optional<Customer> findOptionalById(String id) {
    Optional<HCustomer> optionalCustomer = jpaRepository.findById(id);
    return optionalCustomer.map(mapper::toDomain);
  }

  private Customer checkExisting(Customer domain) {
    Optional<HCustomer> optionalCustomer = jpaRepository.findById(domain.getId());
    if (optionalCustomer.isEmpty()) {
      Customer customer =
          domain.toBuilder()
              .recentlyAdded(true)
              .customerType(
                  domain.getCustomerType() == null
                      ? CustomerType.INDIVIDUAL
                      : domain.getCustomerType())
              .build();
      return customer;
    } else {
      HCustomer existing = optionalCustomer.get();
      Customer customer =
          domain.toBuilder()
              .latestFullAddress(existing.getFullAddress())
              .customerType(
                  domain.getCustomerType() == null
                      ? existing.getCustomerType()
                      : domain.getCustomerType())
              .createdAt(existing.getCreatedAt())
              .build();
      return customer;
    }
  }

  private static Sort updatedAtDescSort() {
    return Sort.by(updatedAtDescOrder());
  }

  private static Sort.Order updatedAtDescOrder() {
    return new Sort.Order(Sort.Direction.DESC, UPDATED_AT_PROPERTY);
  }
}
