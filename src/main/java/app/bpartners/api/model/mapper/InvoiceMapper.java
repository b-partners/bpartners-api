package app.bpartners.api.model.mapper;

import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.Product;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.repository.jpa.model.HInvoiceCustomer;
import app.bpartners.api.repository.jpa.model.HProduct;
import app.bpartners.api.service.AccountService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class InvoiceMapper {
  private final InvoiceCustomerMapper customerMapper;
  private final ProductMapper productMapper;
  private final AccountService accountService;

  public Invoice toDomain(
      HInvoice invoice,
      HInvoiceCustomer invoiceCustomer,
      List<HProduct> products) {
    List<Product> actualProducts = List.of();
    if (products != null) {
      actualProducts = products.stream()
          .map(productMapper::toDomain)
          .collect(Collectors.toUnmodifiableList());
    }
    return Invoice.builder()
        .id(invoice.getId())
        .ref(invoice.getRef())
        .title(invoice.getTitle())
        .products(actualProducts)
        .sendingDate(invoice.getSendingDate())
        .toPayAt(invoice.getToPayAt())
        .invoiceCustomer(customerMapper.toDomain(invoiceCustomer))
        .account(accountService.getAccounts().get(0))
        .status(invoice.getStatus())
        .build();
  }

  public HInvoice toEntity(Invoice domain) {
    return HInvoice.builder()
        .id(domain.getId())
        .ref(domain.getRef())
        .title(domain.getTitle())
        .idAccount(domain.getAccount().getId())
        .sendingDate(domain.getSendingDate())
        .toPayAt(domain.getToPayAt())
        .status(domain.getStatus())
        .build();
  }
}
