package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreateInvoiceRelaunch;
import app.bpartners.api.endpoint.rest.model.InvoiceRelaunch;
import app.bpartners.api.endpoint.rest.validator.CreateInvoiceRelaunchValidator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class InvoiceRelaunchRestMapper {
  private final CreateInvoiceRelaunchValidator validator;

  public InvoiceRelaunch toRest(app.bpartners.api.model.InvoiceRelaunch invoiceRelaunch) {
    return new InvoiceRelaunch()
        .id(invoiceRelaunch.getId())
        .createdDatetime(invoiceRelaunch.getCreatedDatetime())
        .unpaidRelaunch(invoiceRelaunch.getUnpaidRelaunch())
        .draftRelaunch(invoiceRelaunch.getDraftRelaunch());
  }

  public app.bpartners.api.model.InvoiceRelaunch toDomain(
      CreateInvoiceRelaunch createInvoiceRelaunch) {
    validator.accept(createInvoiceRelaunch);
    return app.bpartners.api.model.InvoiceRelaunch.builder()
        .draftRelaunch(createInvoiceRelaunch.getDraftRelaunch())
        .unpaidRelaunch(createInvoiceRelaunch.getUnpaidRelaunch())
        .build();
  }
}
