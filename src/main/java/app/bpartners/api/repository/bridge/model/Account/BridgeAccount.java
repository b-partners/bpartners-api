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

import static app.bpartners.api.endpoint.rest.model.AccountStatus.INVALID_CREDENTIALS;
import static app.bpartners.api.endpoint.rest.model.AccountStatus.OPENED;
import static app.bpartners.api.endpoint.rest.model.AccountStatus.UNKNOWN;
import static app.bpartners.api.endpoint.rest.model.AccountStatus.VALIDATION_REQUIRED;

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
    switch (this.status) {
      case 0:
        return OPENED;
      case 402:
        return INVALID_CREDENTIALS;
      case 1100:
        return VALIDATION_REQUIRED;
      default:
        return UNKNOWN;
    }
  }
}
