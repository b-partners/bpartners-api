package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.InvoiceReference;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class InvoiceReferenceValidator implements Consumer<InvoiceReference> {
  @Override
  public void accept(InvoiceReference invoiceReference) {
    if (invoiceReference == null) {
      throw new BadRequestException("InvoiceReference is mandatory");
    } else {
      if (invoiceReference.getNewReference() == null) {
        throw new BadRequestException("InvoiceReference.newReference is mandatory");
      }
    }
  }
}
