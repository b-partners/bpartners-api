package app.bpartners.api.model.validator;


import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.jpa.model.HInvoice;
import org.springframework.stereotype.Component;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.DRAFT;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;

@Component
public class InvoiceRelaunchValidator {
  public void accept(HInvoice actual) {
    if (actual.getStatus().equals(DRAFT)
        || actual.getStatus().equals(PAID)) {
      throw new BadRequestException("Invoice." + actual.getId() + " actual status is "
          + actual.getStatus() + " and it cannot be relaunched");
    }
  }
}
