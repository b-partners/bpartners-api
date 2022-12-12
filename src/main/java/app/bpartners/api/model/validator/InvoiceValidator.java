package app.bpartners.api.model.validator;


import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.InvoiceRepository;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.DRAFT;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PROPOSAL;

@Component
@AllArgsConstructor
public class InvoiceValidator implements Consumer<Invoice> {
  private final InvoiceRepository repository;

  @Override
  public void accept(Invoice actual) {
    Optional<Invoice> optionalInvoice = repository.getOptionalById(actual.getId());
    String actualInvoiceId = "Invoice." + actual.getId();

    if (optionalInvoice.isEmpty()) {
      if (!actual.getStatus().equals(DRAFT)) {
        throw new BadRequestException(
            actualInvoiceId + " does not exist yet and can only have " + DRAFT
                + " status");
      }
    } else {
      Invoice persisted = optionalInvoice.get();
      if (persisted.getStatus().equals(PAID)) {
        throw new BadRequestException(actualInvoiceId + " was already paid");
      } else {
        checkDraftInvoice(actual, persisted, actualInvoiceId);
        checkProposalInvoice(actual, persisted, actualInvoiceId);
        checkConfirmedInvoice(actual, persisted, actualInvoiceId);
      }
    }
  }

  private static void validateAttributes(Invoice actual) {
    StringBuilder builder = new StringBuilder();
    if (actual.getInvoiceCustomer() == null) {
      builder.append("Customer is mandatory. ");
    }
    if (actual.getSendingDate() == null) {
      builder.append("Sending date is mandatory. ");
    }
    if (actual.getRef() == null) {
      builder.append("Reference is mandatory. ");
    }
    if (actual.getToPayAt() == null) {
      builder.append("Payment date is mandatory. ");
    }
    String message = builder.toString();
    if (!message.isEmpty()) {
      throw new BadRequestException(message);
    }
  }

  private static void checkDraftInvoice(Invoice actual, Invoice persisted, String invoiceId) {
    if (persisted.getStatus().equals(DRAFT)) {
      if (actual.getStatus().equals(PROPOSAL)) {
        //TODO: add test to this products and amount validator (DRAFT -> PROPOSAL)
        validateProductsAmount(actual);
        validateAttributes(actual);
      }
      if (!actual.getStatus().equals(DRAFT) && !actual.getStatus().equals(PROPOSAL)) {
        throw new BadRequestException(getCustomStatusMessage(invoiceId, persisted.getStatus(),
            DRAFT, " or " + PROPOSAL));
      }
    }
  }

  private static void validateProductsAmount(Invoice actual) {
    StringBuilder builder = new StringBuilder();
    if (actual.getProducts().isEmpty()) {
      builder.append(actual.getStatus())
          .append(" invoice with empty products can not be validated. ");
    } else {
      int totalPrice = actual.getProducts().stream()
          .mapToInt(product -> product.getQuantity() * product.getUnitPrice().getCentsRoundUp())
          .sum();
      if (totalPrice == 0) {
        builder.append(actual.getStatus())
            .append(" invoice with zero amount can not be validated. ");
      }
    }
    String messageException = builder.toString();
    if (!messageException.isEmpty()) {
      throw new BadRequestException(messageException);
    }
  }

  private static void checkProposalInvoice(Invoice actual, Invoice persisted, String invoiceId) {
    if (persisted.getStatus().equals(PROPOSAL)) {
      if (!actual.getStatus().equals(CONFIRMED)) {
        throw new BadRequestException(
            getStatusMessage(invoiceId, persisted.getStatus(), CONFIRMED));
      } else {
        //TODO: add test to this products and amount validator (PROPOSAL -> CONFIRMED)
        validateProductsAmount(actual);
        if (!actual.equals(persisted)) {
          throw new BadRequestException(invoiceId + " was already sent and "
              + "can not be modified anymore");
        }
      }
    }
  }

  private static void checkConfirmedInvoice(Invoice actual, Invoice persisted, String invoiceId) {
    if (persisted.getStatus().equals(CONFIRMED)) {
      if (!actual.getStatus().equals(PAID)) {
        throw new BadRequestException(getStatusMessage(invoiceId, persisted.getStatus(), PAID));
      } else {
        if (!actual.equals(persisted)) {
          throw new BadRequestException(invoiceId + " was already confirmed"
              + " and can not be modified anymore");
        }
      }
    }
  }

  private static String getStatusMessage(
      String invoiceId, InvoiceStatus persisted, InvoiceStatus actual) {
    return invoiceId + " actual status is "
        + persisted + " and can only become " + actual;
  }

  private static String getCustomStatusMessage(
      String invoiceId, InvoiceStatus persisted, InvoiceStatus actual, String customMessage) {
    return getStatusMessage(invoiceId, persisted, actual) + customMessage;
  }
}
