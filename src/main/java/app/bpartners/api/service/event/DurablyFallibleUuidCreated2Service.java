package app.bpartners.api.service.event;

import static java.lang.Thread.sleep;

import app.bpartners.api.PojaGenerated;
import app.bpartners.api.endpoint.event.model.DurablyFallibleUuidCreated2;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@PojaGenerated
@SuppressWarnings("all")
@Service
@AllArgsConstructor
@Slf4j
public class DurablyFallibleUuidCreated2Service implements Consumer<DurablyFallibleUuidCreated2> {
  private final UuidCreatedService uuidCreatedService;

  @SneakyThrows
  @Override
  public void accept(DurablyFallibleUuidCreated2 durablyFallibleUuidCreated2) {
    sleep(durablyFallibleUuidCreated2.getWaitDurationBeforeConsumingInSeconds() * 1_000L);
    if (durablyFallibleUuidCreated2.shouldFail()) {
      throw new RuntimeException("Oops, random fail!");
    }

    uuidCreatedService.accept(durablyFallibleUuidCreated2.getUuidCreated());
  }
}
