package app.bpartners.api.unit.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.bpartners.api.model.credit.CreditTransaction;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CreditTransactionTest {

  @Test
  void creation_datetime_is_exposed_at_the_precision_the_database_keeps() {
    var actual =
        CreditTransaction.builder()
            .creationDatetime(Instant.parse("2026-08-01T00:00:00.123456789Z"))
            .build();

    assertEquals(Instant.parse("2026-08-01T00:00:00.123Z"), actual.getCreationDatetime());
  }

  @Test
  void missing_creation_datetime_stays_null() {
    assertNull(CreditTransaction.builder().build().getCreationDatetime());
  }
}
