package app.bpartners.api.service.invoice;

import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.CustomerRepository;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CustomerInvoiceValidator implements Consumer<Invoice> {
  private final CustomerRepository customerRepository;

  @Override
  public void accept(Invoice invoice) {
    if (invoice.getCustomer() != null) {
      var customerId = invoice.getCustomer().getId();
      customerRepository
          .findOptionalById(customerId)
          .orElseThrow(() -> new NotFoundException("Customer(id=" + customerId + ") not found"));
    } else {
      throw new BadRequestException("Invoice.customer is mandatory");
    }
  }
}
