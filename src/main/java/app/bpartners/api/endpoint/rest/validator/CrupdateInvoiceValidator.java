package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.CrupdateInvoice;
import app.bpartners.api.model.exception.BadRequestException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static javax.xml.bind.DatatypeConverter.parseDateTime;

@Slf4j
@Component
public class CrupdateInvoiceValidator implements Consumer<CrupdateInvoice> {

  @Override
  public void accept(CrupdateInvoice invoice) {
    StringBuilder exceptionBuilder = new StringBuilder();

    if (invoice.getStatus() == null) {
      exceptionBuilder.append("Status is mandatory. ");
    } else {
      if (invoice.getRef() != null) {
        String reference = invoice.getRef();
        if (reference.isBlank()) {
          invoice.setRef(null);
        }
      }
    }
    if (isBadSendingDate(invoice)) {
      log.warn("Bad sending date, actual = " + invoice.getSendingDate() + ", today="
          + todayDateWithTimeZone(invoice));
      exceptionBuilder.append("Invoice can not be sent no later than today. ");
    }
    if (isBadPaymentAndSendingDate(invoice)) {
      exceptionBuilder.append("Payment date can not be before the sending date. ");
    }

    String exceptionMessage = exceptionBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }

  private boolean isBadSendingDate(CrupdateInvoice invoice) {
    LocalDate today = todayDateWithTimeZone(invoice);
    return invoice.getSendingDate() != null && invoice.getSendingDate().compareTo(today) != 0
        && invoice.getSendingDate().isAfter(today);
  }

  private static boolean isBadPaymentAndSendingDate(CrupdateInvoice invoice) {
    return invoice.getToPayAt() != null && invoice.getSendingDate() != null
        && invoice.getToPayAt().isBefore(invoice.getSendingDate());
  }

  private LocalDate todayDateWithTimeZone(CrupdateInvoice invoice) {
    LocalDate sendingDate = invoice.getSendingDate();
    LocalDate validityDate = invoice.getValidityDate();
    ZoneId localZoneId = parseDateTime(Objects.requireNonNullElse(
        sendingDate, validityDate).toString()).getTimeZone().toZoneId();
    return LocalDate.ofInstant(Instant.now(), localZoneId);
  }

}
