package app.bpartners.api.service.wms.model;

import java.util.List;
import lombok.Data;

@Data
public class AirbusPNEOResponse {
  private boolean error;
  private int itemsPerPage;
  private int startIndex;
  private int totalResults;
  private String type;
  private List<AirbusFeature> features;
}
