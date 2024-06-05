package app.bpartners.api.endpoint.event.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.time.Duration;
import javax.annotation.processing.Generated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Generated("EventBridge")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@ToString
public class RefreshUserInvoiceSummaryTriggered extends PojaEvent {
  @JsonProperty("userId")
  private String userId;

  @Override
  public Duration maxDuration() {
    return Duration.ofMinutes(1);
  }

  @Override
  public Duration maxBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }
}
