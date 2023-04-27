package app.bpartners.api.repository.bridge.model.Item;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
@Data
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

  @Override
  public String toString() {
    return "Item(id=" + id + ",bankId=" + bankId + ",status=" + status + ")";
  }
}
