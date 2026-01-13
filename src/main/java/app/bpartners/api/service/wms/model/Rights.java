package app.bpartners.api.service.wms.model;

import java.util.Map;
import lombok.Data;

@Data
public class Rights {
  private Map<String, Object> browse;
  private Map<String, Object> wms;
  private Map<String, Object> wmts;
}
