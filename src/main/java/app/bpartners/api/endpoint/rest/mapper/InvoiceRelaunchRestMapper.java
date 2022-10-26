package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreateInvoiceRelaunchConf;
import app.bpartners.api.endpoint.rest.model.InvoiceRelaunchConf;
import app.bpartners.api.endpoint.rest.validator.CreateInvoiceRelaunchValidator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class InvoiceRelaunchRestMapper {
  private final CreateInvoiceRelaunchValidator validator;

  public InvoiceRelaunchConf toRest(
      app.bpartners.api.model.InvoiceRelaunchConf invoiceRelaunchConf) {
    return new InvoiceRelaunchConf()
        .id(invoiceRelaunchConf.getId())
        .updatedAt(invoiceRelaunchConf.getUpdatedAt())
        .unpaidRelaunch(invoiceRelaunchConf.getUnpaidRelaunch())
        .draftRelaunch(invoiceRelaunchConf.getDraftRelaunch());
  }

  public app.bpartners.api.model.InvoiceRelaunchConf toDomain(
      CreateInvoiceRelaunchConf createInvoiceRelaunchConf) {
    validator.accept(createInvoiceRelaunchConf);
    return app.bpartners.api.model.InvoiceRelaunchConf.builder()
        .draftRelaunch(createInvoiceRelaunchConf.getDraftRelaunch())
        .unpaidRelaunch(createInvoiceRelaunchConf.getUnpaidRelaunch())
        .build();
  }
}
