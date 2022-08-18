package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.AccountRestMapper;
import app.bpartners.api.endpoint.rest.model.Account;
import app.bpartners.api.service.AccountService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class AccountController {
  private final AccountService accountService;
  private final AccountRestMapper mapper;

  @GetMapping("/account")
  public Account getAccount() {
    return mapper.toRest(accountService.getAccount());
  }
}
