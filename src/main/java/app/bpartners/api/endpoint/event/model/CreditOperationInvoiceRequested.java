package app.bpartners.api.endpoint.event.model;

import app.bpartners.api.model.credit.CreditTransaction;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode(callSuper = false)
@Builder
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CreditOperationInvoiceRequested extends PojaEvent {
  private CreditTransaction creditTransaction;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(5L);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1L);
  }
}
