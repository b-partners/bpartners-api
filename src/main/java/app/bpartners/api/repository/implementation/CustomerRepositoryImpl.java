package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.security.principal.PrincipalProvider;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.CustomerMapper;
import app.bpartners.api.repository.CustomerRepository;
import app.bpartners.api.repository.jpa.CustomerJpaRepository;
import app.bpartners.api.repository.jpa.model.HCustomer;
import app.bpartners.api.service.AccountService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
@Slf4j
public class CustomerRepositoryImpl implements CustomerRepository {
  private final CustomerJpaRepository jpaRepository;
  private final CustomerMapper mapper;

  private final PrincipalProvider provider;

  private final AccountService accountService;

  private final EntityManager entityManager;


  @Override
  public List<Customer> findByAccountIdAndName(
      String accountId, String firstName, String lastName, int page, int pageSize) {
    Pageable pageable = PageRequest.of(page, pageSize);
    return jpaRepository.findByIdAccountAndFirstNameAndLastNameContainingIgnoreCase(
            accountId, firstName, lastName, pageable).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<Customer> findByAccountIdAndCriteria(String accountId, String firstname,
                                                   String lastname, String email,
                                                   String phoneNumber, String city,
                                                   String country, int page, int pageSize) {
    Pageable pageable = PageRequest.of(page, pageSize);
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<HCustomer> query = builder.createQuery(HCustomer.class);
    Root<HCustomer> root = query.from(HCustomer.class);
    List<Predicate> predicates = new ArrayList<>();
    Predicate[] arrays =
        retrieveNotNullPredicates(
            accountId, firstname, lastname,
            email, phoneNumber, city,
            country, builder, root, predicates);

    query
        .where(builder.and(predicates.toArray(arrays)))
        .orderBy(QueryUtils.toOrders(pageable.getSort(), root, builder));

    return entityManager.createQuery(query)
        .setFirstResult((pageable.getPageNumber()) * pageable.getPageSize())
        .setMaxResults(pageable.getPageSize())
        .getResultList()
        .stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  private static Predicate[] retrieveNotNullPredicates(
      String accountId, String firstname, String lastname,
      String email, String phoneNumber, String city,
      String country, CriteriaBuilder builder,
      Root<HCustomer> root, List<Predicate> predicates) {

    predicates.add(
        builder.equal(root.get("idAccount"), accountId)
    );
    if (firstname != null) {
      predicates.add(builder.or(
          builder.like(root.get("firstName"), "%" + firstname + "%"),
          builder.like(builder.lower(root.get("firstName")),
              "%" + firstname + "%")
      ));
    }
    if (lastname != null) {
      predicates.add(builder.or(
          builder.like(builder.lower(root.get("lastName")), "%" + lastname + "%"),
          builder.like(root.get("lastName"), "%" + lastname + "%")
      ));
    }

    if (email != null) {
      predicates.add(builder.or(
          builder.like(builder.lower(root.get("email")), "%" + email + "%"),
          builder.like(root.get("email"), "%" + email + "%")
      ));
    }
    if (phoneNumber != null) {
      predicates.add(builder.or(
          builder.like(builder.lower(root.get("phone")), "%" + phoneNumber + "%"),
          builder.like(root.get("phone"), "%" + phoneNumber + "%")
      ));
    }
    if (city != null) {
      predicates.add(
          builder.or(
              builder.like(builder.lower(root.get("city")), "%" + city + "%"),
              builder.like(root.get("city"), "%" + city + "%")
          ));
    }
    if (country != null) {
      predicates.add(builder.or(
          builder.like(builder.lower(root.get("country")), "%" + country + "%"),
          builder.like(root.get("country"), "%" + country + "%")
      ));
    }
    return new Predicate[predicates.size()];
  }

  @Override
  public List<Customer> findByAccount(String accountId, int page, int pageSize) {
    Pageable pageable = PageRequest.of(page, pageSize);
    return jpaRepository.findAllByIdAccount(accountId, pageable).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<Customer> saveAll(String accountId, List<Customer> toCreate) {
    List<HCustomer> entityToCreate = toCreate.stream()
        .map(this::crupdate)
        .map(mapper::toEntity)
        .collect(Collectors.toUnmodifiableList());
    return jpaRepository.saveAll(entityToCreate).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public Customer findById(String id) {
    Optional<HCustomer> customerTemplate = jpaRepository.findById(id);
    if (customerTemplate.isPresent()) {
      return mapper.toDomain(customerTemplate.get());
    } else {
      throw new NotFoundException("Customer." + id + " is not found.");
    }
  }

  private Customer crupdate(Customer customer) {
    if (customer.getId() != null) {
      Optional<HCustomer> persisted = jpaRepository.findById(customer.getId());
      if (persisted.isPresent()) {
        persisted.get().setIdAccount(customer.getIdAccount());
        persisted.get().setFirstName(customer.getFirstName());
        persisted.get().setLastName(customer.getLastName());
        persisted.get().setCity(customer.getCity());
        persisted.get().setCountry(customer.getCountry());
        persisted.get().setAddress(customer.getAddress());
        persisted.get().setEmail(customer.getEmail());
        persisted.get().setPhone(customer.getPhone());
        persisted.get().setWebsite(customer.getWebsite());
        persisted.get().setZipCode(customer.getZipCode());
        return mapper.toDomain(persisted.get());
      } else {
        throw new NotFoundException("Customer." + customer.getId() + " is not found.");
      }
    } else {
      return customer;
    }
  }
}
