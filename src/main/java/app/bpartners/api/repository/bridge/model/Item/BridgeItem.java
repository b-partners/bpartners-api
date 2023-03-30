package app.bpartners.api.repository.bridge.model.Item;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class BridgeItem {

  @JsonProperty("id")
  private Long id;
  @JsonProperty("status")
  private Integer status;
  @JsonProperty("status_code_info")
  private String statusCodeInfo;
  @JsonProperty("status_code_description")
  private String statusCodeDescription;
  @JsonProperty("bank_id")
  private Long bankId;
}
