package app.bpartners.api.endpoint.rest.controller.health;

import static app.bpartners.api.endpoint.rest.controller.health.PingController.KO;
import static app.bpartners.api.endpoint.rest.controller.health.PingController.OK;
import static java.lang.Thread.sleep;
import static java.util.UUID.randomUUID;

import app.bpartners.api.PojaGenerated;
import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.DurablyFallibleUuidCreated;
import app.bpartners.api.endpoint.event.model.UuidCreated;
import app.bpartners.api.repository.DummyUuidRepository;
import app.bpartners.api.repository.model.DummyUuid;
import java.util.ArrayList;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@PojaGenerated
@RestController
@AllArgsConstructor
public class HealthEventController {

  DummyUuidRepository dummyUuidRepository;
  EventProducer eventProducer;

  @GetMapping(value = "/health/event")
  public ResponseEntity<String> random_durably_fallible_uuid_are_fired_then_created(
      @RequestParam(defaultValue = "1") int nbEvent,
      @RequestParam(defaultValue = "2") int waitInSeconds)
      throws InterruptedException {
    if (nbEvent < 1 || nbEvent > 500) {
      throw new RuntimeException("nbEvent must be between 1 and 500");
    }
    var uuids = new ArrayList<String>();
    for (int i = 0; i < nbEvent; i++) {
      uuids.add(randomUUID().toString());
    }

    eventProducer.accept(
        uuids.stream()
            .map(
                uuid ->
                    (Object)
                        DurablyFallibleUuidCreated.builder()
                            .uuidCreated(new UuidCreated(uuid))
                            .failureRate(0.1)
                            .waitDurationBeforeConsumingInSeconds(waitInSeconds)
                            .build())
            .toList());

    sleep(20_000);
    var savedUuids = dummyUuidRepository.findAllById(uuids).stream().map(DummyUuid::getId).toList();
    return savedUuids.containsAll(uuids) ? OK : KO;
  }
}
