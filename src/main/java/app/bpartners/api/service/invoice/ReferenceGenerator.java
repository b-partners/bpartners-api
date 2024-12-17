package app.bpartners.api.service.invoice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ReferenceGenerator {
  public static String generateReference() {
    LocalDateTime now = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyyyyHHmmss");
    String formattedDateTime = now.format(formatter);

    return "REF-" + formattedDateTime;
  }
}
