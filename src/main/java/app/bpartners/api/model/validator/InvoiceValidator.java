package app.bpartners.api.model.validator;

import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.exception.ForbiddenException;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.DRAFT;

@Component
@AllArgsConstructor
public class InvoiceValidator implements Consumer<Invoice> {
  private final InvoiceJpaRepository repository;

  @Override
  public void accept(Invoice invoice) {
    Optional<HInvoice> actual = repository.findById(invoice.getId());
    if (actual.isPresent()) {
      if (!invoice.getStatus().equals(DRAFT)) {
        if (actual.get().getStatus().equals(CONFIRMED)) {
          throw new ForbiddenException("Action not permitted");
        }
      } else if (invoice.getStatus().equals(DRAFT)
          && !actual.get().getStatus().equals(DRAFT)) {
        throw new ForbiddenException("Action not permitted");
      }
    }
  }
}
