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
    StringBuilder message = new StringBuilder();
    if (invoice.getTitle() == null) {
      message.append("Title is mandatory. ");
    }
    if (invoice.getRef() == null) {
      message.append("Reference is mandatory. ");
    }
    if (invoice.getCustomer() == null) {
      message.append("Customer is mandatory. ");
    }
    if (invoice.getSendingDate() == null) {
      message.append("Sending date is mandatory. ");
    }
    if (invoice.getToPayAt() == null) {
      message.append("Payment date is mandatory. ");
    }
    if (invoice.getSendingDate().isAfter(today)) {
      message.append("Invoice can not be sent no later than today. ");
    }
    if (invoice.getProducts() == null) {
      message.append("Products are mandatory. ");
    }
    if (invoice.getToPayAt().isBefore(invoice.getSendingDate())) {
      message.append("Payment date can not be after the sending date. ");
    }
    String exceptionMessage = message.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
