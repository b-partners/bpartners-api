package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CrupdateInvoice;
import app.bpartners.api.endpoint.rest.model.CrupdateInvoiceGlobalReduction;
import app.bpartners.api.endpoint.rest.model.Invoice;
import app.bpartners.api.endpoint.rest.model.InvoiceContent;
import app.bpartners.api.service.AccountService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class InvoiceRestMapper {
  private final CustomerRestMapper customerMapper;
  private final InvoiceContentRestMapper contentRestMapper;
  private final AccountService accountService;

  public Invoice toRest(app.bpartners.api.model.Invoice domain) {
    List<app.bpartners.api.model.InvoiceContent> domainContent = domain.getContent();
    List<InvoiceContent> restContent = null;
    if (domainContent != null) {
      restContent = domainContent.stream()
          .map(contentRestMapper::toRest)
          .collect(Collectors.toUnmodifiableList());
    }
    return new Invoice()
        .id(domain.getId())
        .ref(domain.getRef())
        .title(domain.getTitle())
        .customer(customerMapper.toRest(domain.getCustomer()))
        .status(domain.getStatus())
        .content(restContent)
        .vat(domain.getVat())
        .globalReduction(new CrupdateInvoiceGlobalReduction()
            .amount(domain.getAmountReduction())
            .percentage(domain.getPercentageReduction()))
        .invoiceDate(domain.getInvoiceDate())
        .toPayAt(domain.getToPayAt());
  }

  public app.bpartners.api.model.Invoice toDomain(
      String accountId, String id, CrupdateInvoice rest) {
    List<InvoiceContent> restContent = rest.getContent();
    List<app.bpartners.api.model.InvoiceContent> domainContent = null;
    if (restContent != null) {
      domainContent = restContent.stream()
          .map(contentRestMapper::toDomain)
          .collect(Collectors.toUnmodifiableList());
    }
    return app.bpartners.api.model.Invoice.builder()
        .id(id)
        .title(rest.getTitle())
        .ref(rest.getRef())
        .vat(rest.getVat())
        .invoiceDate(rest.getInvoiceDate())
        .toPayAt(rest.getToPayAt())
        .percentageReduction(rest.getGlobalReduction().getPercentage())
        .amountReduction(rest.getGlobalReduction().getAmount())
        .customer(customerMapper.toDomain(accountId, rest.getCustomer()))
        .account(accountService.getAccounts().get(0)) //TODO: change to getAccountById
        .content(domainContent)
        .status(rest.getStatus())
        .build();
  }
}
