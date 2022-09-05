package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.TransactionCategoryRestMapper;
import app.bpartners.api.endpoint.rest.model.CreateTransactionCategory;
import app.bpartners.api.endpoint.rest.model.TransactionCategory;
import app.bpartners.api.service.TransactionCategoryService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class TransactionCategoryController {
  private final TransactionCategoryService service;
  private final TransactionCategoryRestMapper restMapper;

  @GetMapping("/transactionCategories")
  public List<TransactionCategory> getTransactionCategories() {
    return service.getTransactionCategories().stream()
        .map(restMapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }

  @PostMapping("/transactionCategories")
  public List<TransactionCategory> createTransactionCategories(
      @RequestBody List<CreateTransactionCategory> toCreate) {
    List<app.bpartners.api.model.TransactionCategory> domainToCreate = toCreate.stream()
        .map(restMapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
    return service.createTransactionCategories(domainToCreate).stream()
        .map(restMapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }
}
