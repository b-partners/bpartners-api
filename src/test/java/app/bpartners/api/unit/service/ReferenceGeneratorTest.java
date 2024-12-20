package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.api.service.invoice.ReferenceGenerator;
import java.time.LocalDateTime;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class ReferenceGeneratorTest {

  @Test
  void testGet() {
    LocalDateTime fixedDateTime = LocalDateTime.of(2024, 12, 19, 15, 30, 45);
    Supplier<LocalDateTime> fixedDateTimeSupplier = () -> fixedDateTime;
    ReferenceGenerator subject = new ReferenceGenerator(fixedDateTimeSupplier);
    var expected = "REF-19122024153045";

    String actual = subject.get();

    assertEquals(expected, actual);
  }
}
