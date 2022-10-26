package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.endpoint.rest.security.principal.PrincipalProvider;
import app.bpartners.api.model.InvoiceRelaunch;
import app.bpartners.api.repository.jpa.model.HInvoiceRelaunch;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class InvoiceRelaunchMapper {
  private final PrincipalProvider provider;

  public InvoiceRelaunch toDomain(HInvoiceRelaunch entity) {
    return InvoiceRelaunch.builder()
        .id(entity.getId())
        .draftRelaunch(entity.getDraftRelaunch())
        .unpaidRelaunch(entity.getUnpaidRelaunch())
        .createdDatetime(entity.getCreatedDatetime())
        .build();
  }

  public HInvoiceRelaunch toEntity(InvoiceRelaunch domain) {
    return HInvoiceRelaunch.builder()
        .id(domain.getId())
        .accountId(((Principal) provider.getAuthentication().getPrincipal()).getAccount().getId())
        .draftRelaunch(domain.getDraftRelaunch())
        .unpaidRelaunch(domain.getUnpaidRelaunch())
        .build();
  }
}
