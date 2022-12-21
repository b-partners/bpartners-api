package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.security.AuthenticatedResourceProvider;
import app.bpartners.api.model.AccountInvoiceRelaunchConf;
import app.bpartners.api.model.InvoiceRelaunchConf;
import app.bpartners.api.repository.jpa.model.HAccountInvoiceRelaunchConf;
import app.bpartners.api.repository.jpa.model.HInvoiceRelaunchConf;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class InvoiceRelaunchConfMapper {
  private final AuthenticatedResourceProvider authResourceProvider;

  public AccountInvoiceRelaunchConf toDomain(HAccountInvoiceRelaunchConf entity) {
    return AccountInvoiceRelaunchConf.builder()
        .id(entity.getId())
        .draftRelaunch(entity.getDraftRelaunch())
        .unpaidRelaunch(entity.getUnpaidRelaunch())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }

  public final InvoiceRelaunchConf toDomain(HInvoiceRelaunchConf entity) {
    return InvoiceRelaunchConf
        .builder()
        .id(entity.getId())
        .idInvoice(entity.getIdInvoice())
        .delay(entity.getDelay())
        .rehearsalNumber(entity.getRehearsalNumber())
        .build();
  }

  public HAccountInvoiceRelaunchConf toEntity(AccountInvoiceRelaunchConf domain) {
    return HAccountInvoiceRelaunchConf.builder()
        .id(domain.getId())
        .accountId(authResourceProvider.getAccount().getId())
        .draftRelaunch(domain.getDraftRelaunch())
        .unpaidRelaunch(domain.getUnpaidRelaunch())
        .build();
  }

  public HInvoiceRelaunchConf toEntity(InvoiceRelaunchConf domain) {
    return HInvoiceRelaunchConf
        .builder()
        .id(domain.getId())
        .idInvoice(domain.getIdInvoice())
        .delay(domain.getDelay())
        .rehearsalNumber(domain.getRehearsalNumber())
        .build();
  }
}
