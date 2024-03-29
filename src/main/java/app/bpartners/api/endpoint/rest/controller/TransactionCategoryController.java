package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.TransactionCategoryRestMapper;
import app.bpartners.api.endpoint.rest.model.CreateTransactionCategory;
import app.bpartners.api.endpoint.rest.model.TransactionCategory;
import app.bpartners.api.endpoint.rest.model.TransactionTypeEnum;
import app.bpartners.api.endpoint.rest.validator.DateFilterValidator;
import app.bpartners.api.service.TransactionCategoryService;
import java.time.LocalDate;
import java.util.List;
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
  private final DateFilterValidator dateValidator;

  @GetMapping("/accounts/{accountId}/transactionCategories")
  public List<TransactionCategory> getTransactionCategories(
      @PathVariable String accountId,
      @RequestParam(name = "from") String startDateValue,
      @RequestParam(name = "to") String endDateValue,
      @RequestParam(name = "transactionType", required = false)
          TransactionTypeEnum transactionType) {
    LocalDate startDate = LocalDate.parse(startDateValue);
    LocalDate endDate = LocalDate.parse(endDateValue);
    dateValidator.accept(startDate, endDate);
    return service.getCategoryTemplates(accountId, transactionType, startDate, endDate).stream()
        .map(mapper::toRest)
        .toList();
  }

  @PostMapping("/accounts/{accountId}/transactions/{transactionId}/transactionCategories")
  public List<TransactionCategory> createTransactionCategories(
      @PathVariable String accountId,
      @PathVariable String transactionId,
      @RequestBody List<CreateTransactionCategory> toCreate) {
    List<app.bpartners.api.model.TransactionCategory> domainToCreate =
        toCreate.stream()
            .map(category -> mapper.toDomain(transactionId, accountId, category))
            .toList();
    return service.createCategories(domainToCreate).stream().map(mapper::toRest).toList();
  }
}
