package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.RelaunchType;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceRelaunch;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.jpa.model.HInvoiceRelaunch;
import app.bpartners.api.service.AttachmentService;
import app.bpartners.api.service.InvoiceService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class InvoiceRelaunchMapper {
  private final InvoiceMapper invoiceMapper;
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
      Invoice invoice, String object, String htmlBody, boolean userRelaunched) {
    return HInvoiceRelaunch.builder()
        .invoice(invoiceMapper.toEntity(invoice, false))
        .isUserRelaunched(userRelaunched)
        .type(mapRelaunchType(invoice))
        .object(object)
        .emailBody(htmlBody)
        .attachmentFileId(invoice.getFileId())
        .build();
  }

  private RelaunchType mapRelaunchType(Invoice invoice) {
    switch (invoice.getStatus()) {
      case PROPOSAL:
        return RelaunchType.PROPOSAL;
      case CONFIRMED:
        return RelaunchType.CONFIRMED;
      default:
        throw new BadRequestException(
            "Invoice." + invoice.getId() + " with status " + invoice.getStatus() + "can not be "
                + "relaunched");
    }
  }
}
