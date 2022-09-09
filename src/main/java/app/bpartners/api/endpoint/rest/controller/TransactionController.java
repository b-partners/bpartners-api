package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.TransactionRestMapper;
import app.bpartners.api.endpoint.rest.model.Transaction;
import app.bpartners.api.service.TransactionService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class TransactionController {
  private final TransactionService service;
  private final TransactionRestMapper mapper;

  @GetMapping(value = "/accounts/{id}/transactions")
  public List<Transaction> getTransactions(@PathVariable(name = "id") String accountId) {
    return service.getTransactionsByAccountId(accountId).stream()
        .map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }

  @PatchMapping(value = "/accounts/{aId}/transactions/{tId}")
  public Transaction modifyTransaction(
      @PathVariable(name = "aId") String accountId,
      @PathVariable(name = "tId") String transactionId,
      @RequestBody Transaction newTransactionValue) {
    return mapper.toRest(
        service.modifyTransaction(transactionId, mapper.toDomain(newTransactionValue)));
  }
}
