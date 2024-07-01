package app.bpartners.api.endpoint.event.consumer.model;

import app.bpartners.api.PojaGenerated;
import lombok.AllArgsConstructor;
import lombok.Getter;

@PojaGenerated
@SuppressWarnings("all")
@AllArgsConstructor
public class ConsumableEvent {
  @Getter private final TypedEvent event;
  private final Runnable acknowledger;
  private final Runnable randomVisibilityTimeoutSetter;

  public void ack() {
    acknowledger.run();
  }

  public void newRandomVisibilityTimeout() {
    randomVisibilityTimeoutSetter.run();
  }
}
