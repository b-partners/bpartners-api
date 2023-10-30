package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.AccountInvoiceRelaunchConf;
import app.bpartners.api.endpoint.rest.model.Attachment;
import app.bpartners.api.endpoint.rest.model.CreateAccountInvoiceRelaunchConf;
import app.bpartners.api.endpoint.rest.model.EmailInfo;
import app.bpartners.api.endpoint.rest.model.InvoiceRelaunch;
import app.bpartners.api.endpoint.rest.model.InvoiceRelaunchConf;
import app.bpartners.api.endpoint.rest.validator.CreateInvoiceRelaunchConfValidator;
import app.bpartners.api.model.UserInvoiceRelaunchConf;
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

  public UserInvoiceRelaunchConf toDomain(
      CreateAccountInvoiceRelaunchConf createAccountInvoiceRelaunchConf) {
    validator.accept(createAccountInvoiceRelaunchConf);
    return UserInvoiceRelaunchConf.builder()
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
      UserInvoiceRelaunchConf userInvoiceRelaunchConf) {
    return new AccountInvoiceRelaunchConf()
        .id(userInvoiceRelaunchConf.getId())
        .updatedAt(userInvoiceRelaunchConf.getUpdatedAt())
        .unpaidRelaunch(userInvoiceRelaunchConf.getUnpaidRelaunch())
        .draftRelaunch(userInvoiceRelaunchConf.getDraftRelaunch());
  }

  public InvoiceRelaunch toRest(app.bpartners.api.model.InvoiceRelaunch domain, String accountId) {
    return toRest(domain).accountId(accountId);
  }

  public InvoiceRelaunch toRest(app.bpartners.api.model.InvoiceRelaunch domain) {
    List<Attachment> attachments =
        domain.getAttachments().stream()
            .map(attachmentRestMapper::toRest)
            .toList();
    return new InvoiceRelaunch()
        .id(domain.getId())
        .type(domain.getType())
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
