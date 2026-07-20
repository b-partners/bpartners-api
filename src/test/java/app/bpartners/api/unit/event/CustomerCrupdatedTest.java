package app.bpartners.api.unit.event;

import static app.bpartners.api.endpoint.event.model.CustomerCrupdated.Type.CREATE;
import static app.bpartners.api.endpoint.event.model.CustomerCrupdated.Type.UPDATE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.api.endpoint.event.model.CustomerCrupdated;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class CustomerCrupdatedTest {
  ObjectMapper om = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  @Test
  void serializes_type() throws JsonProcessingException {
    var event = new CustomerCrupdated().type(CREATE);

    var json = om.readTree(om.writeValueAsString(event));

    assertEquals(CREATE.toString(), json.get("type").asText());
  }

  @Test
  void deserializes_type() throws JsonProcessingException {
    var event = om.readValue("{\"type\":\"UPDATE\"}", CustomerCrupdated.class);

    assertEquals(UPDATE, event.getType());
  }

  @Test
  void type_survives_serialization_round_trip() throws JsonProcessingException {
    var event = new CustomerCrupdated().type(CREATE);

    var deserialized = om.readValue(om.writeValueAsString(event), CustomerCrupdated.class);

    assertEquals(CREATE, deserialized.getType());
  }
}
