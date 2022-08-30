package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.AccountHolderRestMapper;
import app.bpartners.api.endpoint.rest.model.AccountHolder;
import app.bpartners.api.service.AccountHolderService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class AccountHolderController {

  private final AccountHolderService accountHolderService;
  private final AccountHolderRestMapper accountHolderMapper;

  @GetMapping("/users/{userId}/accounts/{accountId}/accountHolders")
  public List<AccountHolder> getAccountHolders(
      @PathVariable String accountId,
      @PathVariable String userId) {
    return accountHolderService.getAccountHolders().stream()
        .map(accountHolderMapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }
}
