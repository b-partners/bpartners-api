package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.AccountRestMapper;
import app.bpartners.api.endpoint.rest.model.Account;
import app.bpartners.api.endpoint.rest.model.AccountValidationRedirection;
import app.bpartners.api.endpoint.rest.model.BankConnectionRedirection;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.model.UpdateAccountIdentity;
import app.bpartners.api.endpoint.rest.validator.UpdateAccountIdentityRestValidator;
import app.bpartners.api.service.AccountService;
import java.util.List;
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
  public List<Account> getAccountsByUserId(@PathVariable(name = "id") String userId) {
    return service.getAccountsByUserId(userId).stream()
        .map(mapper::toRest)
        .toList();
  }

  //TODO: check redirection urls
  @PostMapping("/users/{userId}/accounts/{accountId}/initiateAccountValidation")
  public AccountValidationRedirection initiateAccountValidation(
      @PathVariable(name = "userId") String userId,
      @PathVariable(name = "accountId") String accountId,
      @RequestBody RedirectionStatusUrls redirectionUrls) {
    return new AccountValidationRedirection()
        .redirectionUrl(service.initiateAccountValidation(accountId))
        .redirectionStatusUrls(new RedirectionStatusUrls()
            .successUrl(redirectionUrls.getSuccessUrl())
            .failureUrl(redirectionUrls.getFailureUrl()));
  }

  @PostMapping("/users/{userId}/accounts/{accountId}/initiateBankConnection")
  public BankConnectionRedirection initiateBankConnection(
      @PathVariable String userId,
      @PathVariable String accountId,
      @RequestBody(required = false) RedirectionStatusUrls urls) {
    return service.initiateBankConnection(userId, urls);
  }

  @PostMapping("/users/{userId}/initiateBankConnection")
  public BankConnectionRedirection initiateBankConnectionWithoutAccount(
      @PathVariable String userId,
      @RequestBody(required = false) RedirectionStatusUrls urls) {
    return service.initiateBankConnection(userId, urls);
  }

  @PostMapping("/users/{userId}/disconnectBank")
  public Account disconnectBank(@PathVariable String userId) {
    return mapper.toRest(service.disconnectBank(userId));
  }

  @PutMapping("/users/{userId}/accounts/{accountId}/identity")
  public Account updateAccountIdentity(
      @PathVariable String userId,
      @PathVariable String accountId,
      @RequestBody UpdateAccountIdentity accountIdentity) {
    validator.accept(accountIdentity);
    app.bpartners.api.model.UpdateAccountIdentity toSave =
        mapper.toDomain(accountId, accountIdentity);
    return mapper.toRest(service.updateAccountIdentity(toSave));
  }
}
