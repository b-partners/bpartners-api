package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.api.service.invoice.ReferenceGenerator;
import java.time.LocalDateTime;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class ReferenceGeneratorTest {
  @Test
  void testGenerateReference() {
    LocalDateTime fixedDateTime = LocalDateTime.of(2024, 12, 19, 15, 30, 45);
    Supplier<LocalDateTime> fixedDateTimeSupplier = () -> fixedDateTime;
    ReferenceGenerator generator = new ReferenceGenerator(fixedDateTimeSupplier);

    String reference = generator.generateReference();

    assertEquals("REF-19122024153045", reference);
  }
}
