package app.bpartners.api.model.validator;


import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.Product;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.InvoiceRepository;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.DRAFT;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PROPOSAL;

@Component
@AllArgsConstructor
@Slf4j
public class InvoiceValidator implements Consumer<Invoice> {
  private final InvoiceRepository repository;

  @Override
  public void accept(Invoice actual) {
    Optional<Invoice> persisted = repository.getOptionalById(actual.getId());
    if (persisted.isEmpty() && !actual.getStatus().equals(DRAFT)) {
      throw new BadRequestException(
          "Invoice." + actual.getId() + " does not exist yet and can only have " + DRAFT
              + " status");
    } else if (persisted.isPresent()) {
      Invoice persistedValue = persisted.get();
      if (persistedValue.getStatus().equals(PAID)) {
        throw new BadRequestException("Invoice." + actual.getId() + " was already paid");
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
            persistedValue.setProducts(ignoreIds(persistedValue.getProducts()));
            persistedValue.getInvoiceCustomer().setId(null);
            if (!actual.equals(persistedValue)) {
              throw new BadRequestException("Invoice." + actual.getId() + " was already sent and "
                  + "can not be modified anymore");
            }
          }
        }
        if (persistedValue.getStatus().equals(CONFIRMED)
            && !actual.getStatus().equals(PAID)) {
          throw new BadRequestException("Invoice." + actual.getId() + " actual status is "
              + persistedValue.getStatus() + " and can only become " + PAID);
        }
      }
    }
  }

  private List<Product> ignoreIds(List<Product> productList) {
    return productList.stream()
        .peek(product -> product.setId(null))
        .collect(Collectors.toUnmodifiableList());
  }
}
