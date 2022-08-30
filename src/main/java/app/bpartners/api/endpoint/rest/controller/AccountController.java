package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.AccountRestMapper;
import app.bpartners.api.endpoint.rest.model.Account;
import app.bpartners.api.service.AccountService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class AccountController {
  private AccountService service;
  private AccountRestMapper mapper;

  @GetMapping("/users/{id}/accounts")
  public List<Account> getAccounts(@PathVariable(name = "id") String userId) {
    return service.getAccounts().stream()
        .map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }
}
