package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.Product;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.repository.jpa.model.HInvoiceCustomer;
import app.bpartners.api.repository.jpa.model.HProduct;
import app.bpartners.api.service.AccountService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import static java.util.UUID.randomUUID;

@Component
@AllArgsConstructor
public class InvoiceMapper {
  private final InvoiceCustomerMapper customerMapper;
  private final InvoiceJpaRepository jpaRepository;
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
        .fileId(invoice.getFileId())
        .title(invoice.getTitle())
        .comment(invoice.getComment())
        .products(actualProducts)
        .sendingDate(invoice.getSendingDate())
        .updatedAt(invoice.getUpdatedAt())
        .toPayAt(invoice.getToPayAt())
        .invoiceCustomer(customerMapper.toDomain(invoiceCustomer))
        .account(accountService.getAccountById(invoice.getIdAccount()))
        .status(invoice.getStatus())
        .toBeRelaunched(invoice.isToBeRelaunched())
        .createdAt(invoice.getCreatedDatetime())
        .build();
  }

  public HInvoice toEntity(Invoice domain) {
    Optional<HInvoice> persisted = jpaRepository.findById(domain.getId());
    String fileId = persisted.isPresent() ? persisted.get().getFileId() : domain.getFileId();
    String id = domain.getId();
    if (persisted.isPresent()
        && persisted.get().getStatus() == InvoiceStatus.PROPOSAL
        && domain.getStatus() == InvoiceStatus.CONFIRMED) {
      id = randomUUID().toString();
    }
    return HInvoice.builder()
        .id(id)
        .fileId(fileId)
        .comment(domain.getComment())
        .ref(domain.getRef())
        .title(domain.getTitle())
        .idAccount(domain.getAccount().getId())
        .sendingDate(domain.getSendingDate())
        .toPayAt(domain.getToPayAt())
        .status(domain.getStatus())
        .toBeRelaunched(domain.isToBeRelaunched())
        .build();
  }
}
