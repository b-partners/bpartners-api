package app.bpartners.api.repository.bridge.model.Item;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
@Data
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class BridgeItemStatus {
  @JsonProperty("status")
  private String status;

  @JsonProperty("refreshed_at")
  private Instant refreshedAt;

  @JsonProperty("refreshed_accounts_count")
  private Integer refreshedCount;

  @JsonProperty("total_accounts_count")
  private Integer accountsToBeRefreshedCount;
}
