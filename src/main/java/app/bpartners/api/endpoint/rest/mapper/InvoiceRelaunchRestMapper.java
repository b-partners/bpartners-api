package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreateInvoiceRelaunchConf;
import app.bpartners.api.endpoint.rest.model.InvoiceRelaunch;
import app.bpartners.api.endpoint.rest.model.InvoiceRelaunchConf;
import app.bpartners.api.endpoint.rest.validator.CreateInvoiceRelaunchConfValidator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class InvoiceRelaunchRestMapper {
  private final CreateInvoiceRelaunchConfValidator validator;
  private final InvoiceRestMapper invoiceRestMapper;

  public app.bpartners.api.model.InvoiceRelaunchConf toDomain(
      CreateInvoiceRelaunchConf createInvoiceRelaunchConf) {
    validator.accept(createInvoiceRelaunchConf);
    return app.bpartners.api.model.InvoiceRelaunchConf.builder()
        .draftRelaunch(createInvoiceRelaunchConf.getDraftRelaunch())
        .unpaidRelaunch(createInvoiceRelaunchConf.getUnpaidRelaunch())
        .build();
  }

  public InvoiceRelaunchConf toRest(
      app.bpartners.api.model.InvoiceRelaunchConf invoiceRelaunchConf) {
    return new InvoiceRelaunchConf()
        .id(invoiceRelaunchConf.getId())
        .updatedAt(invoiceRelaunchConf.getUpdatedAt())
        .unpaidRelaunch(invoiceRelaunchConf.getUnpaidRelaunch())
        .draftRelaunch(invoiceRelaunchConf.getDraftRelaunch());
  }

  public InvoiceRelaunch toRest(app.bpartners.api.model.InvoiceRelaunch domain) {
    return new InvoiceRelaunch()
        .id(domain.getId())
        .type(domain.getType())
        .invoice(invoiceRestMapper.toRest(domain.getInvoice()))
        .accountId(domain.getAccountId())
        .isUserRelaunched(domain.isUserRelaunched())
        .creationDatetime(domain.getCreationDatetime());
  }
}
