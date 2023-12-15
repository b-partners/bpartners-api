package app.bpartners.api.repository.bridge.model.Account;

import static app.bpartners.api.endpoint.rest.model.AccountStatus.INVALID_CREDENTIALS;
import static app.bpartners.api.endpoint.rest.model.AccountStatus.OPENED;
import static app.bpartners.api.endpoint.rest.model.AccountStatus.SCA_REQUIRED;
import static app.bpartners.api.endpoint.rest.model.AccountStatus.UNDERGOING_REFRESHMENT;
import static app.bpartners.api.endpoint.rest.model.AccountStatus.UNKNOWN;
import static app.bpartners.api.endpoint.rest.model.AccountStatus.VALIDATION_REQUIRED;

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

@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
@Builder(toBuilder = true)
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class BridgeAccount {
  public static final int BRIDGE_STATUS_OK = 0;
  public static final int BRIDGE_STATUS_WRONG_CRED = 402;
  public static final int BRIDGE_STATUS_SCA = 1010;
  public static final int BRIDGE_STATUS_VALIDATION_REQ = 1100;
  public static final int BRIDGE_ACCOUNT_ADDED_RECENTLY = -2;
  public static final int BRIDGE_CREDENTIAL_UPDATED_RECENTLY = -3;

  @JsonProperty("id")
  private String id;

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
      case BRIDGE_STATUS_OK:
        return OPENED;
      case BRIDGE_STATUS_WRONG_CRED:
        return INVALID_CREDENTIALS;
      case BRIDGE_STATUS_SCA:
        return SCA_REQUIRED;
      case BRIDGE_STATUS_VALIDATION_REQ:
        return VALIDATION_REQUIRED;
      case BRIDGE_ACCOUNT_ADDED_RECENTLY:
      case BRIDGE_CREDENTIAL_UPDATED_RECENTLY:
        return UNDERGOING_REFRESHMENT;
      default:
        return UNKNOWN;
    }
  }
}
