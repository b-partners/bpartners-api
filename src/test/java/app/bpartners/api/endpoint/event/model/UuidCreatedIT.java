package app.bpartners.api.endpoint.event.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.api.PojaGenerated;
import app.bpartners.api.conf.FacadeIT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@PojaGenerated
class UuidCreatedIT extends FacadeIT {

  @Autowired ObjectMapper om;

  @Test
  public void serialize_then_deserialize() throws JsonProcessingException {
    var uuid = new UuidCreated("dummy");

    var serialized = om.writeValueAsString(uuid);
    var deserialized = om.readValue(serialized, UuidCreated.class);

    assertEquals(uuid, deserialized);
    assertEquals("dummy", deserialized.getUuid());
    assertEquals(Duration.ofSeconds(10), deserialized.maxDuration());
    assertEquals(Duration.ofSeconds(30), deserialized.maxBackoffBetweenRetries());
  }
}
