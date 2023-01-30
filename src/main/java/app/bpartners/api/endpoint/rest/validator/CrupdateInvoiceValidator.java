package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.CrupdateInvoice;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class CrupdateInvoiceValidator implements Consumer<CrupdateInvoice> {
  public static final String REGION_PARIS = "Europe/Paris";
  private final LocalDate today = LocalDate.ofInstant(Instant.now(), ZoneId.of(REGION_PARIS));

  @Override
  public void accept(CrupdateInvoice invoice) {
    StringBuilder exceptionBuilder = new StringBuilder();

    if (invoice.getStatus() == null) {
      exceptionBuilder.append("Status is mandatory. ");
    }
    if (invoice.getRef() != null && invoice.getRef().isBlank()) {
      invoice.setRef(null);
    }
    if (isBadPaymentAndSendingDate(invoice)) {
      exceptionBuilder.append("Payment date can not be after the sending date. ");
    }
    if (isBadSendingDate(invoice)) {
      log.warn("Bad sending date, actual = " + invoice.getSendingDate() + ", today=" + today);
      //TODO: uncomment if any warn message is logged anymore
      //exceptionBuilder.append("Invoice can not be sent no later than today. ");
    }

    String exceptionMessage = exceptionBuilder.toString();
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
