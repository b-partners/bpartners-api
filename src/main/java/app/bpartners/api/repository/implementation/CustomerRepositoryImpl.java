package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.security.principal.PrincipalProvider;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.CustomerMapper;
import app.bpartners.api.repository.CustomerRepository;
import app.bpartners.api.repository.jpa.CustomerJpaRepository;
import app.bpartners.api.repository.jpa.model.HCustomer;
import app.bpartners.api.service.AccountService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class CustomerRepositoryImpl implements CustomerRepository {
  private final CustomerJpaRepository jpaRepository;
  private final CustomerMapper mapper;

  private final PrincipalProvider provider;

  private final AccountService accountService;

  public static String convertNullToEmptyString(String input) {
    return input == null ? "" : input;
  }

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
    //TODO : use API Criteria instead
    return jpaRepository.findByIdAccountAndFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCaseAndEmailContainingIgnoreCaseAndPhoneContainingIgnoreCaseAndCityContainingIgnoreCaseAndCountryContainingIgnoreCase(
            accountId, convertNullToEmptyString(firstname), convertNullToEmptyString(lastname),
            convertNullToEmptyString(email), convertNullToEmptyString(phoneNumber),
            convertNullToEmptyString(city), convertNullToEmptyString(country), pageable).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
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
