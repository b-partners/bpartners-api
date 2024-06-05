package app.bpartners.api.endpoint.event.model;

import java.io.Serializable;
import java.time.Duration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = false)
@Builder
@ToString
@AllArgsConstructor
public class RelaunchHoldersProspectTriggered extends PojaEvent {
    @Override
    public Duration maxDuration() {
        return Duration.ofMinutes(10);
    }

    @Override
    public Duration maxBackoffBetweenRetries() {
        return Duration.ofMinutes(1);
    }
}
