package app.bpartners.api.endpoint.event.model;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import java.time.Duration;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInvoiceStatusRequested extends PojaEvent {
  private String invoiceIdentifier;
  private String invoiceReference;
  private InvoiceStatus newStatus;
  private String userOwnerIdentifier;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(1);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }
}
