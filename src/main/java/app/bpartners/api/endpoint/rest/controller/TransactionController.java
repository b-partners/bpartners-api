package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.TransactionRestMapper;
import app.bpartners.api.endpoint.rest.mapper.TransactionsSummaryRestMapper;
import app.bpartners.api.endpoint.rest.model.Transaction;
import app.bpartners.api.endpoint.rest.model.TransactionStatus;
import app.bpartners.api.endpoint.rest.model.TransactionsSummary;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.service.TransactionService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class TransactionController {
  private final TransactionService service;
  private final TransactionRestMapper mapper;
  private final TransactionsSummaryRestMapper summaryRestMapper;

  @GetMapping(value = "/accounts/{id}/transactions")
  public List<Transaction> getTransactions(
      @PathVariable(name = "id") String accountId,
      @RequestParam(name = "page", required = false)
      PageFromOne page,
      @RequestParam(name = "pageSize", required = false)
      BoundedPageSize pageSize,
      @RequestParam(name = "label", required = false) String label,
      @RequestParam(name = "status", required = false) TransactionStatus status,
      @RequestParam(name = "category", required = false) String category) {
    return service.getPersistedByIdAccount(accountId, label, status, category, page, pageSize)
        .stream()
        .map(mapper::toRest)
        .toList();
  }

  @GetMapping(value = "/accounts/{id}/transactions/{tId}")
  public Transaction getTransactionById(
      @PathVariable(name = "id") String accountId,
      @PathVariable(name = "tId") String transactionId
  ) {
    return mapper.toRest(service.getById(transactionId));
  }

  @GetMapping(value = "/accounts/{aId}/transactionsSummary")
  public TransactionsSummary getTransactionsSummary(
      @PathVariable(name = "aId") String accountId,
      @RequestParam(required = false) Integer year) {
    String idUser =
        AuthProvider.getAuthenticatedUserId(); //TODO: should be changed when endpoint changed
    return summaryRestMapper.toRest(service.getTransactionsSummary(idUser, year));
  }

  @PutMapping(value = "/accounts/{accountId}/transactions/{transactionId}/invoices/{invoiceId}")
  public Transaction justifyTransaction(
      @PathVariable String accountId,
      @PathVariable String transactionId,
      @PathVariable String invoiceId) {
    return mapper.toRest(service.justifyTransaction(transactionId, invoiceId));
  }
}
