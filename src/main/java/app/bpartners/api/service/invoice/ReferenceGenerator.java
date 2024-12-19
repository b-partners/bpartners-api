package app.bpartners.api.service.invoice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

public class ReferenceGenerator {
  private final Supplier<LocalDateTime> dateTimeSupplier;

  // Constructor to allow dependency injection of the dateTimeSupplier
  public ReferenceGenerator(Supplier<LocalDateTime> dateTimeSupplier) {
    this.dateTimeSupplier = dateTimeSupplier;
  }

  public String generateReference() {
    LocalDateTime now = dateTimeSupplier.get();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyyyyHHmmss");
    String formattedDateTime = now.format(formatter);

    return "REF-" + formattedDateTime;
  }
}
