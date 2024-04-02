package app.bpartners.api.endpoint.event.gen;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import javax.annotation.processing.Generated;
import lombok.*;

@Generated("EventBridge")
@EqualsAndHashCode
@Builder
@ToString
@AllArgsConstructor
@Data
public class RefreshUserInvoiceSummaryTriggered implements Serializable {
  @JsonProperty("userId")
  private String userId;
}
