package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.CrupdateInvoice;
import app.bpartners.api.model.exception.BadRequestException;
import java.time.LocalDate;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class CrupdateInvoiceValidator implements Consumer<CrupdateInvoice> {
  private final LocalDate today = LocalDate.now();

  @Override
  public void accept(CrupdateInvoice invoice) {
    if (invoice.getTitle() == null) {
      throw new BadRequestException("Title is mandatory");
    }
    if (invoice.getRef() == null) {
      throw new BadRequestException("Reference is mandatory");
    }
    if (invoice.getVat() == null) {
      throw new BadRequestException("Vat is mandatory");
    }
    if (invoice.getCustomer() == null) {
      throw new BadRequestException("Customer is mandatory");
    }
    if (invoice.getSendingDate() == null) {
      throw new BadRequestException("Sending date is mandatory");
    }
    if (invoice.getToPayAt() == null) {
      throw new BadRequestException("`To Pay At` date is mandatory");
    }
    if (invoice.getSendingDate().isAfter(today)) {
      throw new BadRequestException("Invoice can be sent by today");
    }
    if (invoice.getToPayAt().isBefore(invoice.getSendingDate())) {
      throw new BadRequestException("`To Pay At` date can not be after the sending date");
    }
  }
}
