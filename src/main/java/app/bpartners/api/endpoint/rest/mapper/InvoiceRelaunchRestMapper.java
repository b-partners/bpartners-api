package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.AccountInvoiceRelaunchConf;
import app.bpartners.api.endpoint.rest.model.Attachment;
import app.bpartners.api.endpoint.rest.model.CreateAccountInvoiceRelaunchConf;
import app.bpartners.api.endpoint.rest.model.EmailInfo;
import app.bpartners.api.endpoint.rest.model.InvoiceRelaunch;
import app.bpartners.api.endpoint.rest.model.InvoiceRelaunchConf;
import app.bpartners.api.endpoint.rest.validator.CreateInvoiceRelaunchConfValidator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class InvoiceRelaunchRestMapper {
  private final CreateInvoiceRelaunchConfValidator validator;
  private final InvoiceRestMapper invoiceRestMapper;
  private final AttachmentRestMapper attachmentRestMapper;

  public app.bpartners.api.model.AccountInvoiceRelaunchConf toDomain(
      CreateAccountInvoiceRelaunchConf createAccountInvoiceRelaunchConf) {
    validator.accept(createAccountInvoiceRelaunchConf);
    return app.bpartners.api.model.AccountInvoiceRelaunchConf.builder()
        .draftRelaunch(createAccountInvoiceRelaunchConf.getDraftRelaunch())
        .unpaidRelaunch(createAccountInvoiceRelaunchConf.getUnpaidRelaunch())
        .build();
  }

  public app.bpartners.api.model.InvoiceRelaunchConf toDomain(
      InvoiceRelaunchConf rest, String invoiceId
  ) {
    return app.bpartners.api.model.InvoiceRelaunchConf.builder()
        .id(rest.getId())
        .idInvoice(invoiceId)
        .delay(rest.getDelay())
        .rehearsalNumber(rest.getRehearsalNumber())
        .build();
  }

  public AccountInvoiceRelaunchConf toRest(
      app.bpartners.api.model.AccountInvoiceRelaunchConf accountInvoiceRelaunchConf) {
    return new AccountInvoiceRelaunchConf()
        .id(accountInvoiceRelaunchConf.getId())
        .updatedAt(accountInvoiceRelaunchConf.getUpdatedAt())
        .unpaidRelaunch(accountInvoiceRelaunchConf.getUnpaidRelaunch())
        .draftRelaunch(accountInvoiceRelaunchConf.getDraftRelaunch());
  }

  public InvoiceRelaunch toRest(app.bpartners.api.model.InvoiceRelaunch domain) {
    List<Attachment> attachments =
        domain.getAttachments().stream()
            .map(attachmentRestMapper::toRest)
            .collect(Collectors.toUnmodifiableList());
    return new InvoiceRelaunch()
        .id(domain.getId())
        .type(domain.getType())
        .invoice(invoiceRestMapper.toRest(domain.getInvoice()))
        .accountId(domain.getAccountId())
        .isUserRelaunched(domain.isUserRelaunched())
        .creationDatetime(domain.getCreationDatetime())
        .emailInfo(new EmailInfo()
            .emailBody(domain.getEmailBody())
            .emailObject(domain.getEmailObject())
            .attachmentFileId(domain.getAttachmentFileId()))
        .attachments(attachments);
  }

  public InvoiceRelaunchConf toRest(app.bpartners.api.model.InvoiceRelaunchConf domain) {
    return new InvoiceRelaunchConf()
        .id(domain.getId())
        .idInvoice(domain.getIdInvoice())
        .delay(domain.getDelay())
        .rehearsalNumber(domain.getRehearsalNumber());
  }
}
