package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CrupdateInvoice;
import app.bpartners.api.endpoint.rest.model.Invoice;
import app.bpartners.api.endpoint.rest.model.Product;
import app.bpartners.api.endpoint.rest.validator.CrupdateInvoiceValidator;
import app.bpartners.api.service.AccountService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class InvoiceRestMapper {
  private final CustomerRestMapper customerMapper;
  private final ProductRestMapper productRestMapper;
  private final AccountService accountService;
  private final CrupdateInvoiceValidator crupdateInvoiceValidator;


  public Invoice toRest(app.bpartners.api.model.Invoice domain) {
    List<app.bpartners.api.model.Product> domainContent = domain.getProducts();
    List<Product> products = null;
    if (domainContent != null) {
      products = domainContent.stream()
          .map(productRestMapper::toRest)
          .collect(Collectors.toUnmodifiableList());
    }
    return new Invoice()
        .id(domain.getId())
        .ref(domain.getRef())
        .title(domain.getTitle())
        .customer(customerMapper.toRest(domain.getCustomer()))
        .status(domain.getStatus())
        .products(products)
        .vat(domain.getVat())
        .totalVat(domain.getTotalVat())
        .paymentUrl(domain.getPaymentUrl())
        .totalPriceWithoutVat(domain.getTotalPriceWithoutVat())
        .totalPriceWithVat(domain.getTotalPriceWithVat())
        .sendingDate(domain.getSendingDate())
        .toPayAt(domain.getToPayAt());
  }

  public app.bpartners.api.model.Invoice toDomain(
      String accountId, String id, CrupdateInvoice rest) {
    crupdateInvoiceValidator.accept(rest);
    List<app.bpartners.api.model.Product> domain = null; //TODO: getProducts of
    return app.bpartners.api.model.Invoice.builder()
        .id(id)
        .title(rest.getTitle())
        .ref(rest.getRef())
        .vat(rest.getVat())
        .sendingDate(rest.getSendingDate())
        .toPayAt(rest.getToPayAt())
        .customer(customerMapper.toDomain(accountId, rest.getCustomer()))
        .account(accountService.getAccounts().get(0)) //TODO: change to getAccountById
        .products(domain)
        .status(rest.getStatus())
        .build();
  }
}
