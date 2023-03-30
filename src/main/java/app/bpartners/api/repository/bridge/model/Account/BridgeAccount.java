package app.bpartners.api.repository.bridge.model.Account;

import app.bpartners.api.endpoint.rest.model.AccountStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static app.bpartners.api.endpoint.rest.model.AccountStatus.UNKNOWN;

@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
@Builder
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class BridgeAccount {
  @JsonProperty("id")
  private Long id;
  @JsonProperty("bank_id")
  private Long bankId;
  @JsonProperty("name")
  private String name;
  @JsonProperty("balance")
  private Double balance;
  @JsonProperty("status")
  private Integer status;
  @JsonProperty("iban")
  private String iban;
  @JsonProperty("updated_at")
  private Instant updatedAt;

  public AccountStatus getDomainStatus() {
    return this.getStatus() == 0 ? AccountStatus.OPENED : UNKNOWN;
  }
}
