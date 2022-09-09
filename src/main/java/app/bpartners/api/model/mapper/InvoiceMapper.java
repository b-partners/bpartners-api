package app.bpartners.api.model.mapper;

import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceContent;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.repository.jpa.model.HInvoiceContent;
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

  public InvoiceContent toDomain(HInvoiceContent entity) {
    return InvoiceContent.builder()
        .id(entity.getId())
        .invoice(toDomain(entity.getInvoice()))
        .description(entity.getDescription())
        .price(entity.getPrice())
        .quantity(entity.getQuantity())
        .reduction(reductionMapper.toDomain(entity.getReduction()))
        .build();
  }

  public Invoice toDomain(HInvoice entity) {
    List<HInvoiceContent> entityContent = entity.getContent();
    List<InvoiceContent> domainContent = null;
    if (entityContent != null) {
      domainContent = entityContent.stream()
          .map(this::toDomain)
          .collect(Collectors.toUnmodifiableList());
    }
    return Invoice.builder()
        .id(entity.getId())
        .ref(entity.getRef())
        .vat(entity.getVat())
        .invoiceDate(entity.getInvoiceDate())
        .toPayAt(entity.getToPayAt())
        .percentageReduction(entity.getPercentageReduction())
        .amountReduction(entity.getAmountReduction())
        .customer(customerMapper.toDomain(entity.getCustomer()))
        .account(accountService.getAccounts().get(0))
        .content(domainContent)
        .status(entity.getStatus())
        .build();
  }

  public HInvoiceContent toEntity(InvoiceContent domain) {
    return HInvoiceContent.builder()
        .id(domain.getId())
        .description(domain.getDescription())
        .quantity(domain.getQuantity())
        .price(domain.getPrice())
        .reduction(reductionMapper.toEntity(domain.getReduction()))
        .invoice(toEntity(domain.getInvoice()))
        .build();
  }

  public HInvoice toEntity(Invoice domain) {
    List<InvoiceContent> domainContent = domain.getContent();
    List<HInvoiceContent> entityContent = null;
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
        .invoiceDate(domain.getInvoiceDate())
        .toPayAt(domain.getToPayAt())
        .percentageReduction(domain.getPercentageReduction())
        .amountReduction(domain.getAmountReduction())
        .content(entityContent)
        .status(domain.getStatus())
        .build();
  }
}
