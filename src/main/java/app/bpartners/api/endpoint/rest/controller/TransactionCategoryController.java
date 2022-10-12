package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.TransactionCategoryRestMapper;
import app.bpartners.api.endpoint.rest.model.CreateTransactionCategory;
import app.bpartners.api.endpoint.rest.model.TransactionCategory;
import app.bpartners.api.service.TransactionCategoryService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class TransactionCategoryController {
  private final TransactionCategoryRestMapper mapper;
  private final TransactionCategoryService service;
  //TODO: rename the from/to variables to be more explicit

  @GetMapping("/accounts/{accountId}/transactionCategories")
  public List<TransactionCategory> getTransactionCategories(
      @PathVariable String accountId,
      @RequestParam boolean unique,
      @RequestParam(required = false) Optional<Boolean> userDefined,
      @RequestParam String from,
      @RequestParam String to) {
    return userDefined.map(
            isUserDefined -> service.getCategoriesByAccountAndUserDefined(accountId, unique,
                    isUserDefined, from, to)
                .stream()
                .map(mapper::toRest)
                .collect(Collectors.toUnmodifiableList()))
        .orElseGet(() -> service.getCategoriesByAccount(accountId, unique, from, to).stream()
            .map(mapper::toRest)
            .collect(Collectors.toUnmodifiableList()));
  }

  @PostMapping("/accounts/{accountId}/transactions/{transactionId}/transactionCategories")
  public List<TransactionCategory> createTransactionCategories(
      @PathVariable String accountId,
      @PathVariable String transactionId,
      @RequestBody List<CreateTransactionCategory> toCreate) {
    List<app.bpartners.api.model.TransactionCategory> domainToCreate = toCreate.stream()
        .map(category -> mapper.toDomain(transactionId, accountId, category))
        .collect(Collectors.toUnmodifiableList());
    return service.createCategories(domainToCreate).stream()
        .map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }
}
