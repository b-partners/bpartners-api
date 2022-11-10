package app.bpartners.api.model.validator;


import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.exception.BadRequestException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PROPOSAL;

@Component
@AllArgsConstructor
public class InvoiceRelaunchValidator {

  public void accept(Invoice invoice) {
    InvoiceStatus status = invoice.getStatus();
    if (!status.equals(PROPOSAL) && !status.equals(CONFIRMED)) {
      throw new BadRequestException("Invoice." + invoice.getId() + " actual status is "
          + status + " and it cannot be relaunched");
    }
  }
}
