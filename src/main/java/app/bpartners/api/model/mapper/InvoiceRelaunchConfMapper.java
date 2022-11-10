package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.endpoint.rest.security.principal.PrincipalProvider;
import app.bpartners.api.model.InvoiceRelaunchConf;
import app.bpartners.api.repository.jpa.model.HInvoiceRelaunchConf;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class InvoiceRelaunchConfMapper {
  private final PrincipalProvider provider;

  public InvoiceRelaunchConf toDomain(HInvoiceRelaunchConf entity) {
    return InvoiceRelaunchConf.builder()
        .id(entity.getId())
        .draftRelaunch(entity.getDraftRelaunch())
        .unpaidRelaunch(entity.getUnpaidRelaunch())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }

  public HInvoiceRelaunchConf toEntity(InvoiceRelaunchConf domain) {
    return HInvoiceRelaunchConf.builder()
        .id(domain.getId())
        .accountId(((Principal) provider.getAuthentication().getPrincipal()).getAccount().getId())
        .draftRelaunch(domain.getDraftRelaunch())
        .unpaidRelaunch(domain.getUnpaidRelaunch())
        .build();
  }
}
