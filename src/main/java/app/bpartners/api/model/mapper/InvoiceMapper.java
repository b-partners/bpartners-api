package app.bpartners.api.model.mapper;

import app.bpartners.api.model.Invoice;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.service.AccountService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class InvoiceMapper {
  private final CustomerMapper customerMapper;
  private final AccountService accountService;

  public Invoice toDomain(HInvoice entity) {
    return Invoice.builder()
        .id(entity.getId())
        .ref(entity.getRef())
        .vat(entity.getVat())
        .title(entity.getTitle())
        .sendingDate(entity.getSendingDate())
        .toPayAt(entity.getToPayAt())
        .customer(customerMapper.toDomain(entity.getCustomer()))
        .account(accountService.getAccounts().get(0))
        .status(entity.getStatus())
        .build();
  }

  public HInvoice toEntity(Invoice domain) {
    return HInvoice.builder()
        .id(domain.getId())
        .ref(domain.getRef())
        .title(domain.getTitle())
        .customer(customerMapper.toEntity(domain.getCustomer()))
        .idAccount(domain.getAccount().getId())
        .vat(domain.getVat())
        .sendingDate(domain.getSendingDate())
        .toPayAt(domain.getToPayAt())
        .status(domain.getStatus())
        .build();
  }
}
