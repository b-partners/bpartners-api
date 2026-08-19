package app.bpartners.api.unit.mapper;

import static app.bpartners.api.model.credit.CreditTransactionMovementType.CREDIT;
import static app.bpartners.api.model.credit.CreditTransactionType.SUBSCRIPTION_GRANT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.bpartners.api.endpoint.rest.mapper.CreditTransactionRestMapper;
import app.bpartners.api.endpoint.rest.model.CreditTransactionMovementType;
import app.bpartners.api.endpoint.rest.model.CreditTransactionType;
import app.bpartners.api.model.credit.CreditTransaction;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CreditTransactionRestMapperTest {
  CreditTransactionRestMapper subject = new CreditTransactionRestMapper();

  @Test
  void to_rest_maps_stored_fields() {
    var expiry = Instant.parse("2026-09-01T00:00:00Z");
    var creation = Instant.parse("2026-08-01T00:00:00Z");
    var domain =
        CreditTransaction.builder()
            .id("tx_id")
            .userId("user_id")
            .type(SUBSCRIPTION_GRANT)
            .movementType(CREDIT)
            .credits(20L)
            .expirationDatetime(expiry)
            .creationDatetime(creation)
            .build();

    var actual = subject.toRest(domain);

    assertEquals("tx_id", actual.getId());
    assertEquals(CreditTransactionType.SUBSCRIPTION_GRANT, actual.getType());
    assertEquals(CreditTransactionMovementType.CREDIT, actual.getMovementType());
    assertEquals(20L, actual.getCredits());
    assertEquals(expiry, actual.getExpirationDatetime());
    assertEquals(creation, actual.getCreationDatetime());
    assertNull(actual.getLabel());
  }

  @Test
  void to_domain_type_round_trips_each_value() {
    for (var restType : CreditTransactionType.values()) {
      assertEquals(restType.name(), subject.toDomainType(restType).name());
    }
  }

  @Test
  void to_domain_type_maps_null() {
    assertNull(subject.toDomainType(null));
  }
}
