package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.TransactionRestMapper;
import app.bpartners.api.endpoint.rest.mapper.TransactionsSummaryRestMapper;
import app.bpartners.api.endpoint.rest.model.Transaction;
import app.bpartners.api.endpoint.rest.model.TransactionExportInput;
import app.bpartners.api.endpoint.rest.model.TransactionExportLink;
import app.bpartners.api.endpoint.rest.model.TransactionStatus;
import app.bpartners.api.endpoint.rest.model.TransactionsSummary;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.endpoint.rest.validator.TransactionExportValidator;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.endpoint.rest.model.FileInfo;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.mapper.FileMapper;
import app.bpartners.api.service.TransactionService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class TransactionController {
  private final TransactionService service;
  private final TransactionRestMapper mapper;
  private final TransactionsSummaryRestMapper summaryRestMapper;
  private final TransactionExportValidator exportValidator;
  private final FileMapper fileMapper;

  @GetMapping("/accounts/{aId}/transactions/{tId}/supportingDocuments")
  public List<FileInfo> getTransactionSupportingDocuments(@PathVariable String aId,
                                                          @PathVariable String tId) {
    return service.getSupportingDocuments(tId).stream()
        .map(supportingDoc -> fileMapper.toRest(supportingDoc.getFileInfo()))
        .toList();
  }

  @PostMapping("/accounts/{aId}/transactions/{tId}/supportingDocuments")
  public List<FileInfo> addTransactionSupportingDocuments(@PathVariable String aId,
                                                          @PathVariable String tId,
                                                          @RequestBody byte[] documentAsBytes) {
    return service.addSupportingDocuments(
            AuthProvider.getAuthenticatedUserId(),
            tId,
            documentAsBytes).stream()
        .map(supportingDoc -> fileMapper.toRest(supportingDoc.getFileInfo()))
        .toList();
  }

  @DeleteMapping("/accounts/{aId}/transactions/{tId}/supportingDocuments")
  public List<FileInfo> deleteTransactionSupportingDocuments(@PathVariable String aId,
                                                             @PathVariable String tId,
                                                             @RequestBody
                                                             List<String> supportingDocumentsIds) {
    throw new NotImplementedException("Not supported");
  }

  @PostMapping(value = "/accounts/{id}/transactions/exportLink")
  public TransactionExportLink generateTransactionsExportLink(@PathVariable String id,
                                                              @RequestBody
                                                              TransactionExportInput input) {
    exportValidator.accept(input);
    return mapper.toRest(service.generateTransactionSummaryLink(
        id,
        input.getFrom(), input.getTo(),
        input.getTransactionStatus()));
  }

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
