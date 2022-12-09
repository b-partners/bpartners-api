package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.security.AuthenticatedResourceProvider;
import app.bpartners.api.model.AccountInvoiceRelaunchConf;
import app.bpartners.api.repository.jpa.model.HAccountInvoiceRelaunchConf;
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

  public HAccountInvoiceRelaunchConf toEntity(AccountInvoiceRelaunchConf domain) {
    return HAccountInvoiceRelaunchConf.builder()
        .id(domain.getId())
        .accountId(authResourceProvider.getAccount().getId())
        .draftRelaunch(domain.getDraftRelaunch())
        .unpaidRelaunch(domain.getUnpaidRelaunch())
        .build();
  }
}
