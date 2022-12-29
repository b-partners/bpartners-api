package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.CrupdateInvoice;
import app.bpartners.api.model.exception.BadRequestException;
import java.time.LocalDate;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CrupdateInvoiceValidator implements Consumer<CrupdateInvoice> {
  private final LocalDate today = LocalDate.now();

  @Override
  public void accept(CrupdateInvoice invoice) {
    StringBuilder message = new StringBuilder();

    if (invoice.getStatus() == null) {
      message.append("Status is mandatory. ");
    } else {
      if (invoice.getRef() != null) {
        String reference = invoice.getRef();
        if (reference.isBlank()) {
          invoice.setRef(null);
        }
      }
    }
    if (isBadSendingDate(invoice)) {
      log.warn("Bad sending date, actual = " + invoice.getSendingDate() + ", today=" + today);
      //message.append("Invoice can not be sent no later than today. ");
    }
    if (isBadPaymentAndSendingDate(invoice)) {
      message.append("Payment date can not be after the sending date. ");
    }

    String exceptionMessage = message.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }

  }

  private boolean isBadSendingDate(CrupdateInvoice invoice) {
    return invoice.getSendingDate() != null && invoice.getSendingDate().compareTo(today) != 0
        && invoice.getSendingDate().isAfter(today);
  }

  private static boolean isBadPaymentAndSendingDate(CrupdateInvoice invoice) {
    return invoice.getToPayAt() != null && invoice.getSendingDate() != null
        && invoice.getToPayAt().isBefore(invoice.getSendingDate());
  }
}
