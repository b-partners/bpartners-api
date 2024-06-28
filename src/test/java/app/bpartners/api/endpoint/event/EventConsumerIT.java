package app.bpartners.api.endpoint.event;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.api.PojaGenerated;
import app.bpartners.api.endpoint.event.consumer.EventConsumer;
import app.bpartners.api.endpoint.event.consumer.model.ConsumableEvent;
import app.bpartners.api.endpoint.event.consumer.model.TypedEvent;
import app.bpartners.api.endpoint.event.model.UuidCreated;
import app.bpartners.api.repository.DummyUuidRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@PojaGenerated
@SuppressWarnings("all")
class EventConsumerIT extends app.bpartners.api.integration.conf.MockedThirdParties {

  @Autowired EventConsumer subject;
  @Autowired DummyUuidRepository dummyUuidRepository;
  @Autowired ObjectMapper om;

  @Test
  void uuid_created_is_persisted() throws InterruptedException, JsonProcessingException {
    var uuid = randomUUID().toString();
    var uuidCreated = UuidCreated.builder().uuid(uuid).build();
    var payloadReceived = om.readValue(om.writeValueAsString(uuidCreated), UuidCreated.class);

    subject.accept(
        List.of(
            new ConsumableEvent(
                new TypedEvent(
                    "app.bpartners.api.endpoint.event.model.UuidCreated", payloadReceived),
                () -> {},
                () -> {})));

    Thread.sleep(2_000);
    var saved = dummyUuidRepository.findById(uuid).orElseThrow();
    assertEquals(uuid, saved.getId());
  }
}
