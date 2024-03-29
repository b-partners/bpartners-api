package app.bpartners.api.repository.bridge.model.Item;

import static app.bpartners.api.endpoint.rest.security.bridge.BridgeConf.FRANCE_BANK_COUNTRY_CODE;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode
@Data
@ToString
public class BridgeCreateItem {
  @JsonProperty("prefill_email")
  private String prefillEmail;

  @JsonProperty private final String country = FRANCE_BANK_COUNTRY_CODE;
}
