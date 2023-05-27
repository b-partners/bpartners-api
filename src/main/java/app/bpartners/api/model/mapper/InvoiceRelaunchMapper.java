package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.endpoint.rest.model.RelaunchType;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceRelaunch;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.repository.jpa.model.HInvoiceRelaunch;
import app.bpartners.api.service.AttachmentService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class InvoiceRelaunchMapper {
  private final AttachmentService attachmentService;

  public InvoiceRelaunch toDomain(HInvoiceRelaunch entity, Invoice invoice) {
    return InvoiceRelaunch.builder()
        .id(entity.getId())
        .type(entity.getType())
        .invoice(invoice)
        .isUserRelaunched(entity.isUserRelaunched())
        .creationDatetime(entity.getCreationDatetime())
        .emailObject(entity.getObject())
        .emailBody(entity.getEmailBody())
        .attachmentFileId(entity.getAttachmentFileId())
        .attachments(attachmentService.findAllByIdInvoiceRelaunch(entity.getId()))
        .build();
  }

  public HInvoiceRelaunch toEntity(
      HInvoice invoice, String object, String htmlBody, boolean userRelaunched) {
    return HInvoiceRelaunch.builder()
        .invoice(invoice)
        .isUserRelaunched(userRelaunched)
        .type(mapRelaunchType(invoice.getId(), invoice.getStatus()))
        .object(object)
        .emailBody(htmlBody)
        .attachmentFileId(invoice.getFileId())
        .build();
  }

  private RelaunchType mapRelaunchType(String idInvoice, InvoiceStatus invoiceStatus) {
    switch (invoiceStatus) {
      case PROPOSAL:
        return RelaunchType.PROPOSAL;
      case CONFIRMED:
        return RelaunchType.CONFIRMED;
      default:
        throw new BadRequestException(
            "Invoice." + idInvoice + " with status " + invoiceStatus + "can not be "
                + "relaunched");
    }
  }
}
