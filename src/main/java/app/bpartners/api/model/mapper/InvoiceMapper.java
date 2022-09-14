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
  private final PriceReductionMapper reductionMapper;

  public Product toDomain(HProduct entity) {
    return Product.builder()
        .id(entity.getId())
        .invoice(toDomain(entity.getInvoice()))
        .description(entity.getDescription())
        .unitPrice(entity.getPrice())
        .quantity(entity.getQuantity())
        .reduction(reductionMapper.toDomain(entity.getReduction()))
        .build();
  }

  public Invoice toDomain(HInvoice entity) {
    List<HProduct> entityContent = entity.getProducts();
    List<Product> domainContent = null;
    if (entityContent != null) {
      domainContent = entityContent.stream()
          .map(this::toDomain)
          .collect(Collectors.toUnmodifiableList());
    }
    return Invoice.builder()
        .id(entity.getId())
        .ref(entity.getRef())
        .vat(entity.getVat())
        .sendingDate(entity.getSendingDate())
        .toPayAt(entity.getToPayAt())
        .customer(customerMapper.toDomain(entity.getCustomer()))
        .account(accountService.getAccounts().get(0))
        .products(domainContent)
        .status(entity.getStatus())
        .build();
  }

  public HProduct toEntity(Product domain) {
    return HProduct.builder()
        .id(domain.getId())
        .description(domain.getDescription())
        .quantity(domain.getQuantity())
        .price(domain.getUnitPrice())
        .reduction(reductionMapper.toEntity(domain.getReduction()))
        .invoice(toEntity(domain.getInvoice()))
        .build();
  }

  public HInvoice toEntity(Invoice domain) {
    List<Product> domainContent = domain.getProducts();
    List<HProduct> entityContent = null;
    if (domainContent != null) {
      entityContent = domainContent.stream()
          .map(this::toEntity)
          .collect(Collectors.toUnmodifiableList());
    }
    return HInvoice.builder()
        .id(domain.getId())
        .ref(domain.getRef())
        .customer(customerMapper.toEntity(domain.getCustomer()))
        .idAccount(domain.getAccount().getId())
        .vat(domain.getVat())
        .sendingDate(domain.getSendingDate())
        .toPayAt(domain.getToPayAt())
        .products(entityContent)
        .status(domain.getStatus())
        .build();
  }
}
