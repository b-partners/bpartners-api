package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.security.principal.PrincipalProvider;
import app.bpartners.api.model.CustomerTemplate;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.CustomerMapper;
import app.bpartners.api.repository.CustomerRepository;
import app.bpartners.api.repository.jpa.CustomerJpaRepository;
import app.bpartners.api.repository.jpa.model.HCustomerTemplate;
import app.bpartners.api.service.AccountService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class CustomerRepositoryImpl implements CustomerRepository {
  private final CustomerJpaRepository jpaRepository;
  private final CustomerMapper mapper;

  private final PrincipalProvider provider;

  private final AccountService accountService;

  @Override
  public List<CustomerTemplate> findByAccountIdAndName(String accountId, String name) {
    return jpaRepository.findByIdAccountAndNameContainingIgnoreCase(accountId, name).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<CustomerTemplate> findByAccount(String accountId) {
    return jpaRepository.findAllByIdAccount(accountId).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<CustomerTemplate> saveAll(String accountId, List<CustomerTemplate> toCreate) {
    List<HCustomerTemplate> entityToCreate = toCreate.stream()
        .map(this::crupdate)
        .map(mapper::toEntity)
        .collect(Collectors.toUnmodifiableList());
    return jpaRepository.saveAll(entityToCreate).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public CustomerTemplate findById(String id) {
    Optional<HCustomerTemplate> customerTemplate = jpaRepository.findById(id);
    if (customerTemplate.isPresent()) {
      return mapper.toDomain(customerTemplate.get());
    } else {
      throw new NotFoundException("Customer." + id + " is not found.");
    }
  }

  private CustomerTemplate crupdate(CustomerTemplate customer) {
    if (customer.getCustomerId() != null) {
      Optional<HCustomerTemplate> persisted = jpaRepository.findById(customer.getCustomerId());
      if (persisted.isPresent()) {
        persisted.get().setIdAccount(customer.getIdAccount());
        persisted.get().setName(customer.getName());
        persisted.get().setCity(customer.getCity());
        persisted.get().setCountry(customer.getCountry());
        persisted.get().setAddress(customer.getAddress());
        persisted.get().setEmail(customer.getEmail());
        persisted.get().setPhone(customer.getPhone());
        persisted.get().setWebsite(customer.getWebsite());
        persisted.get().setZipCode(customer.getZipCode());
        return mapper.toDomain(persisted.get());
      } else {
        throw new NotFoundException("Customer." + customer.getCustomerId() + " is not found.");
      }
    } else {
      return customer;
    }
  }
}
