package app.bpartners.api.repository.bridge.model.Account;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class BridgeAccount {
  @JsonProperty("id")
  private String id;
  @JsonProperty("bank_id")
  private String bankId;
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
}
