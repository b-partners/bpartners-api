package app.bpartners.api.endpoint.event.model;

import app.bpartners.api.endpoint.event.EventStack;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Duration;

import static app.bpartners.api.endpoint.event.EventStack.EVENT_STACK_2;

@EqualsAndHashCode(callSuper = false)
@Builder
@ToString
@AllArgsConstructor
public class MonthlyCancelledClientsPayment extends PojaEvent {
    @Override
    public Duration maxConsumerDuration() {
        return Duration.ofSeconds(300L);
    }

    @Override
    public Duration maxConsumerBackoffBetweenRetries() {
        return Duration.ofSeconds(60L);
    }

    @Override
    public EventStack getEventStack() {
        return EVENT_STACK_2;
    }
}
