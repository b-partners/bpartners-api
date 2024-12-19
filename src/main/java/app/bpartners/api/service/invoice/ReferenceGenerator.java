package app.bpartners.api.service.invoice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

public class ReferenceGenerator implements Supplier<String> {
  private final Supplier<LocalDateTime> dateTimeSupplier;

  public ReferenceGenerator(Supplier<LocalDateTime> dateTimeSupplier) {
    this.dateTimeSupplier = dateTimeSupplier;
  }

  @Override
  public String get() {
    LocalDateTime now = dateTimeSupplier.get();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyyyyHHmmss");
    String formattedDateTime = now.format(formatter);

    return "REF-" + formattedDateTime;
  }
}
