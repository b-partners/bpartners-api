package app.bpartners.api.model.mapper;

import app.bpartners.api.model.UserInvoiceRelaunchConf;
import app.bpartners.api.model.InvoiceRelaunchConf;
import app.bpartners.api.repository.jpa.model.HInvoiceRelaunchConf;
import app.bpartners.api.repository.jpa.model.HUserInvoiceRelaunchConf;
import org.springframework.stereotype.Component;

@Component
public class InvoiceRelaunchConfMapper {
  public UserInvoiceRelaunchConf toDomain(HUserInvoiceRelaunchConf entity) {
    return UserInvoiceRelaunchConf.builder()
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

  public HUserInvoiceRelaunchConf toEntity(String idUser, UserInvoiceRelaunchConf domain) {
    return HUserInvoiceRelaunchConf.builder()
        .id(domain.getId())
        .idUser(idUser)
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
