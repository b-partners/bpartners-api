package app.bpartners.api.model.mapper;

import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.Product;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.repository.jpa.model.HProduct;
import app.bpartners.api.service.AccountService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class InvoiceMapper {
  private final CustomerMapper customerMapper;
  private final AccountService accountService;

  private final ProductMapper productMapper;

  public Invoice toDomain(HInvoice entity) {
    return Invoice.builder()
        .id(entity.getId())
        .ref(entity.getRef())
        .vat(entity.getVat())
        .sendingDate(entity.getSendingDate())
        .toPayAt(entity.getToPayAt())
        .customer(customerMapper.toDomain(entity.getCustomer()))
        .account(accountService.getAccounts().get(0))
        .products(getInvoiceProducts(entity))
        .status(entity.getStatus())
        .build();
  }

  public HProduct toEntity(Product domain) {
    return HProduct.builder()
        .id(domain.getId())
        .description(domain.getDescription())
        .quantity(domain.getQuantity())
        .unitPrice(domain.getUnitPrice())
        .invoice(toEntity(domain.getInvoice()))
        .build();
  }

  public HInvoice toEntity(Invoice domain) {
    return HInvoice.builder()
        .id(domain.getId())
        .ref(domain.getRef())
        .customer(customerMapper.toEntity(domain.getCustomer()))
        .idAccount(domain.getAccount().getId())
        .vat(domain.getVat())
        .sendingDate(domain.getSendingDate())
        .toPayAt(domain.getToPayAt())
        .products(getHInvoiceProducts(domain))
        .status(domain.getStatus())
        .build();
  }

  private List<HProduct> getHInvoiceProducts(Invoice invoice) {
    List<Product> domains = invoice.getProducts();
    List<HProduct> entities = null;
    if (domains != null) {
      entities = domains.stream()
          .map(this::toEntity)
          .collect(Collectors.toUnmodifiableList());
    }
    return entities;
  }

  private List<Product> getInvoiceProducts(HInvoice invoice) {
    List<Product> domains = null;
    List<HProduct> entities = invoice.getProducts();
    if (entities != null) {
      domains = entities.stream()
          .map(productMapper::toDomain)
          .collect(Collectors.toUnmodifiableList());
    }
    return domains;
  }
}
