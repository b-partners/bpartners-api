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
        .fileId(domain.getFileId())
        .comment(domain.getComment())
        .ref(domain.getRef())
        .title(domain.getTitle())
        .updatedAt(domain.getUpdatedAt())
        .customer(customerMapper.toRest(domain.getInvoiceCustomer()))
        .status(domain.getStatus())
        .products(products)
        .totalVat(domain.getTotalVat().getApproximatedValue())
        .paymentUrl(domain.getPaymentUrl())
        .totalPriceWithoutVat(domain.getTotalPriceWithoutVat().getApproximatedValue())
        .totalPriceWithVat(domain.getTotalPriceWithVat().getApproximatedValue())
        .sendingDate(domain.getSendingDate())
        .toPayAt(domain.getToPayAt());
  }

  public app.bpartners.api.model.Invoice toDomain(
      String accountId, String id, CrupdateInvoice rest) {
    crupdateInvoiceValidator.accept(rest);

    List<app.bpartners.api.model.Product> domain =
        rest.getProducts() == null ? List.of() : rest.getProducts().stream()
            .map(productRestMapper::toDomain)
            .collect(Collectors.toUnmodifiableList());
    return app.bpartners.api.model.Invoice.builder()
        .id(id)
        .title(rest.getTitle())
        .ref(rest.getRef())
        .comment(rest.getComment())
        .sendingDate(rest.getSendingDate())
        .status(rest.getStatus())
        .toPayAt(rest.getToPayAt())
        .invoiceCustomer(customerMapper.toDomain(accountId, id, rest.getCustomer()))
        .account(accountService.getAccountById(accountId))
        .products(domain)
        .build();
  }
}
