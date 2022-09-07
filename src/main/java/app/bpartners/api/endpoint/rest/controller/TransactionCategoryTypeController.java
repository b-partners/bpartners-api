package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.TransactionCategoryTypeRestMapper;
import app.bpartners.api.endpoint.rest.model.CreateTransactionCategoryType;
import app.bpartners.api.endpoint.rest.model.TransactionCategoryType;
import app.bpartners.api.service.TransactionCategoryTypeService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class TransactionCategoryTypeController {
  private final TransactionCategoryTypeService service;
  private final TransactionCategoryTypeRestMapper restMapper;

  @GetMapping("/transactionCategoryTypes")
  public List<TransactionCategoryType> getTransactionCategories() {
    return service.getCategoryTypes().stream()
        .map(restMapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }

  @PostMapping("/transactionCategoryTypes")
  public List<TransactionCategoryType> createTransactionCategories(
      @RequestBody List<CreateTransactionCategoryType> toCreate) {
    List<app.bpartners.api.model.TransactionCategoryType> domainToCreate = toCreate.stream()
        .map(restMapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
    return service.createCategoryTypes(domainToCreate).stream()
        .map(restMapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }
}
