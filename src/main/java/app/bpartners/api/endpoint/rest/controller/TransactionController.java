package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.TransactionRestMapper;
import app.bpartners.api.endpoint.rest.model.CreateTransactionType;
import app.bpartners.api.endpoint.rest.mapper.TransactionsSummaryRestMapper;
import app.bpartners.api.endpoint.rest.model.Transaction;
import app.bpartners.api.endpoint.rest.model.TransactionTypeEnum;
import app.bpartners.api.endpoint.rest.model.TransactionsSummary;
import app.bpartners.api.service.TransactionService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class TransactionController {
  private final TransactionService service;
  private final TransactionRestMapper mapper;
  private final TransactionsSummaryRestMapper summaryRestMapper;

  @GetMapping(value = "/accounts/{id}/transactions")
  public List<Transaction> getTransactions(@PathVariable(name = "id") String accountId) {
    return service.getTransactionsByAccountId(accountId).stream()
        .map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }

  @PutMapping(value = "/accounts/{id}/transactions/{tId}/type")
  public Transaction updateTransactionType(@PathVariable("id") String accountId,
                                           @PathVariable("tId") String swanTransactionId,
                                           @RequestBody CreateTransactionType createTransactionType
  ) {
    TransactionTypeEnum type = createTransactionType.getType();
    return mapper.toRest(service.updateType(swanTransactionId, type));
  }

  @GetMapping(value = "/accounts/{aId}/transactionsSummary")
  public TransactionsSummary getTransactionsSummary(
      @PathVariable(name = "aId") String accountId,
      @RequestParam(required = false) Integer year) {
    return summaryRestMapper.toRest(service.getTransactionsSummary(year));
  }
}
