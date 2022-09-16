package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.endpoint.rest.security.principal.PrincipalProvider;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.exception.ForbiddenException;
import app.bpartners.api.model.mapper.CustomerMapper;
import app.bpartners.api.repository.CustomerRepository;
import app.bpartners.api.repository.jpa.CustomerJpaRepository;
import app.bpartners.api.repository.jpa.model.HCustomer;
import app.bpartners.api.service.AccountService;
import java.util.List;
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

  public Principal getPrincipal() {
    return (Principal) provider.getAuthentication().getPrincipal();
  }

  @Override
  public List<Customer> findByAccountId(String accountId) {
    String authenticatedUserId = getPrincipal().getUserId();
    List<Account> authenticatedUserAccount =
        accountService.getAccountsByUserId(authenticatedUserId);
    if (authenticatedUserAccount.get(0).getId().equals(accountId)) {
      return jpaRepository.findAllByIdAccount(accountId).stream()
          .map(mapper::toDomain)
          .collect(Collectors.toUnmodifiableList());
    } else {
      throw new ForbiddenException("Access is denied");
    }
  }

  @Override
  public List<Customer> save(String accountId, List<Customer> toCreate) {
    List<HCustomer> entityToCreate = toCreate.stream()
        .map(mapper::toEntity)
        .collect(Collectors.toUnmodifiableList());
    String authenticatedUserId = getPrincipal().getUserId();
    List<Account> authenticatedUserAccount =
        accountService.getAccountsByUserId(authenticatedUserId);
    if (authenticatedUserAccount.get(0).getId().equals(accountId)) {
      return jpaRepository.saveAll(entityToCreate).stream()
          .map(mapper::toDomain)
          .collect(Collectors.toUnmodifiableList());
    } else {
      throw new ForbiddenException("Access is denied");
    }
  }
}
