package app.bpartners.api.model.validator;

import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.InvoiceRepository;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.DRAFT;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PROPOSAL;

@Component
@AllArgsConstructor
@Slf4j
public class InvoiceValidator implements Consumer<Invoice> {
  private final InvoiceRepository repository;

  @Override
  public void accept(Invoice actual) {
    Optional<Invoice> persisted = repository.getOptionalById(actual.getId());
    //TODO: refactor to reduce Cognitive complexity
    if (persisted.isPresent()) {
      Invoice persistedValue = persisted.get();
      if (persistedValue.getStatus().equals(CONFIRMED)) {
        throw new BadRequestException("Invoice." + actual.getId() + " was already confirmed");
      } else {
        if (persistedValue.getStatus().equals(DRAFT)
            && !actual.getStatus().equals(PROPOSAL)) {
          throw new BadRequestException("Invoice." + actual.getId() + " actual status is "
              + persistedValue.getStatus() + " and can only become " + PROPOSAL);
        }
        if (persistedValue.getStatus().equals(PROPOSAL)) {
          if (!actual.getStatus().equals(CONFIRMED)) {
            throw new BadRequestException("Invoice." + actual.getId() + " actual status is "
                + persistedValue.getStatus() + " and can only become " + CONFIRMED);
          } else {
            //TODO: for customer and products, compare the content without the ID
            log.info(actual.toString());
            log.info(persistedValue.toString());
            if (!actual.equals(persistedValue)) {
              throw new BadRequestException("Invoice." + actual.getId() + " was already sent and "
                  + "can not be modified anymore");
            }
          }
        }
      }
    } else {
      if (!actual.getStatus().equals(DRAFT)) {
        throw new BadRequestException(
            "Invoice." + actual.getId() + " does not exist yet and can only have " + DRAFT
                + " status");
      }
    }
  }
}
