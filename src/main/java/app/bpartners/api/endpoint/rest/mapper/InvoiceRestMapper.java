package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CrupdateInvoice;
import app.bpartners.api.endpoint.rest.model.Customer;
import app.bpartners.api.endpoint.rest.model.Invoice;
import app.bpartners.api.endpoint.rest.model.Product;
import app.bpartners.api.endpoint.rest.validator.CrupdateInvoiceValidator;
import app.bpartners.api.repository.CustomerRepository;
import app.bpartners.api.service.AccountService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class InvoiceRestMapper {
  private final CustomerRestMapper customerMapper;
  private final CustomerRepository customerRepository;
  private final ProductRestMapper productRestMapper;
  private final AccountService accountService;
  private final CrupdateInvoiceValidator crupdateInvoiceValidator;

  public Invoice toRest(app.bpartners.api.model.Invoice domain) {
    List<app.bpartners.api.model.InvoiceProduct> domainContent = domain.getProducts();
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
        .createdAt(domain.getCreatedAt())
        .customer(customerMapper.toRest(domain.getCustomer()))
        .status(domain.getStatus())
        .products(products)
        .totalVat(domain.getTotalVat().getCentsRoundUp())
        .paymentUrl(domain.getPaymentUrl())
        .totalPriceWithoutVat(domain.getTotalPriceWithoutVat().getCentsRoundUp())
        .totalPriceWithVat(domain.getTotalPriceWithVat().getCentsRoundUp())
        .sendingDate(domain.getSendingDate())
        .metadata(domain.getMetadata())
        .toPayAt(domain.getToPayAt());
  }

  public app.bpartners.api.model.Invoice toDomain(
      String accountId, String id, CrupdateInvoice rest) {
    crupdateInvoiceValidator.accept(rest);
    app.bpartners.api.model.Invoice.InvoiceBuilder invoiceBuilder =
        app.bpartners.api.model.Invoice.builder();
    List<app.bpartners.api.model.InvoiceProduct> products =
        rest.getProducts() == null ? List.of() : rest.getProducts().stream()
            .map(productRestMapper::toInvoiceDomain)
            .collect(Collectors.toUnmodifiableList());
    Customer restCustomer = rest.getCustomer();
    if (restCustomer != null) {
      app.bpartners.api.model.Customer existingCustomer =
          customerRepository.findById(restCustomer.getId());
      invoiceBuilder.customer(existingCustomer);
    }
    return invoiceBuilder
        .id(id)
        .title(rest.getTitle())
        .ref(rest.getRef())
        .comment(rest.getComment())
        .sendingDate(rest.getSendingDate())
        .status(rest.getStatus())
        .toPayAt(rest.getToPayAt())
        .account(accountService.getAccountById(accountId))
        .products(products)
        .metadata(rest.getMetadata() == null ? Map.of() : rest.getMetadata())
        .build();
  }
}
