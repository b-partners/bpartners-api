package app.bpartners.api.endpoint.event.model;

import app.bpartners.api.endpoint.rest.model.EmailRecipientType;
import java.time.Duration;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@ToString
@EqualsAndHashCode(callSuper = false)
public class EmailRecipientsUpdateRequested extends PojaEvent {
  private String userId;
  private String accountHolderId;
  private EmailRecipientType type;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(60L);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(30L);
  }
}
