package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.AccountRestMapper;
import app.bpartners.api.endpoint.rest.model.Account;
import app.bpartners.api.endpoint.rest.model.BankConnectionRedirection;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.model.UpdateAccountIdentity;
import app.bpartners.api.endpoint.rest.validator.UpdateAccountIdentityRestValidator;
import app.bpartners.api.service.AccountService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class AccountController {
  private final AccountService service;
  private final AccountRestMapper mapper;
  private final UpdateAccountIdentityRestValidator validator;

  @GetMapping("/users/{id}/accounts")
  public List<Account> getAccounts(@PathVariable(name = "id") String userId) {
    return service.getAccountsByUserId(userId).stream()
        .map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }

  @PostMapping("/users/{userId}/accounts/{accountId}/initiateBankConnection")
  public BankConnectionRedirection initiateBankConnection(
      @PathVariable String userId,
      @PathVariable String accountId,
      @RequestBody(required = false) RedirectionStatusUrls urls) {
    return service.getBankConnectionInitUrl(userId, accountId, urls);
  }

  @PutMapping("/users/{userId}/accounts/{accountId}/identity")
  public Account updateAccountIdentity(
      @PathVariable String userId,
      @PathVariable String accountId,
      @RequestBody UpdateAccountIdentity accountIdentity) {
    validator.accept(accountIdentity);
    app.bpartners.api.model.Account toSave = mapper.toDomain(accountId, accountIdentity);
    return mapper.toRest(service.updateAccountIdentity(userId, toSave));
  }
}
