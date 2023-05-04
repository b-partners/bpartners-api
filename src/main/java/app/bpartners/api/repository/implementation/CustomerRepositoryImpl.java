package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.model.CustomerStatus;
import app.bpartners.api.endpoint.rest.model.UpdateCustomerStatus;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.CustomerMapper;
import app.bpartners.api.repository.CustomerRepository;
import app.bpartners.api.repository.jpa.CustomerJpaRepository;
import app.bpartners.api.repository.jpa.model.HCustomer;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class CustomerRepositoryImpl implements CustomerRepository {
  private final CustomerJpaRepository jpaRepository;
  private final CustomerMapper mapper;
  private final EntityManager entityManager;

  private static Predicate[] retrieveNotNullPredicates(
      String idUser, String firstname, String lastname,
      String email, String phoneNumber, String city,
      String country, CustomerStatus status, CriteriaBuilder builder,
      Root<HCustomer> root, List<Predicate> predicates) {

    predicates.add(
        builder.equal(root.get("idUser"), idUser)
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
    if (status != null) {
      predicates.add(
          builder.equal(root.get("status"), status)
      );
    } else {
      predicates.add(
          builder.equal(root.get("status"), CustomerStatus.ENABLED)
      );
    }
    return new Predicate[predicates.size()];
  }

  @Override
  public List<Customer> findByIdUserAndCriteria(String idUser, String firstname,
                                                String lastname, String email,
                                                String phoneNumber, String city,
                                                String country, CustomerStatus status, int page,
                                                int pageSize) {
    Pageable pageable = PageRequest.of(page, pageSize);
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<HCustomer> query = builder.createQuery(HCustomer.class);
    Root<HCustomer> root = query.from(HCustomer.class);
    List<Predicate> predicates = new ArrayList<>();
    Predicate[] arrays =
        retrieveNotNullPredicates(
            idUser, firstname, lastname,
            email, phoneNumber, city,
            country, status, builder, root, predicates);

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

  @Override
  public List<Customer> updateCustomersStatuses(List<UpdateCustomerStatus> customerStatusList) {
    List<HCustomer> customersToUpdate = customerStatusList.stream()
        .map(customerStatus ->
            jpaRepository.findById(customerStatus.getId()).orElseThrow(
                    () -> new NotFoundException("Customer(id=" + customerStatus.getId() + " not found"))
                .toBuilder()
                .status(customerStatus.getStatus())
                .build())
        .collect(Collectors.toList());
    return jpaRepository.saveAll(customersToUpdate).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<Customer> saveAll(List<Customer> toCreate) {
    List<HCustomer> entityToCreate = toCreate.stream()
        .map(this::checkExisting)
        .map(mapper::toEntity)
        .collect(Collectors.toUnmodifiableList());
    return jpaRepository.saveAll(entityToCreate).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public Customer findById(String id) {
    return mapper.toDomain(jpaRepository.findById(id)
        .orElseThrow(() ->
            new NotFoundException("Customer." + id + " is not found.")));
  }

  private Customer checkExisting(Customer domain) {
    //Case of update with ID
    String id = domain.getId();
    if (id != null) {
      jpaRepository.findById(id)
          .orElseThrow(() ->
              new NotFoundException(
                  "Customer." + id + " is not found for User(id=" + domain.getIdUser()
                      + ")"));
    }
    Optional<HCustomer> optionalCustomer =
        jpaRepository.findByIdUserAndEmail(domain.getIdUser(), domain.getEmail());
    return optionalCustomer.isEmpty() ? domain : domain.toBuilder()
        .id(optionalCustomer.get().getId())
        .build();
  }
}
