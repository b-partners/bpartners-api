package app.bpartners.api.repository.prospecting.datasource.buildingpermit.model;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.USE_DEFAULTS;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@Builder
public class BuildingPermitList {
  private int total;
  private int limit;
  private List<BuildingPermit> records;

  @JsonProperty("total")
  @JsonInclude(USE_DEFAULTS)
  public int getTotal() {
    return total;
  }

  @JsonProperty("limit")
  @JsonInclude(USE_DEFAULTS)
  public int getLimit() {
    return limit;
  }

  @JsonProperty("records")
  @JsonInclude(USE_DEFAULTS)
  public List<BuildingPermit> getRecords() {
    return records;
  }
}
