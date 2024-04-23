package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.ZoomLevel;
import app.bpartners.api.service.WMS.MapLayer;
import app.bpartners.api.service.WMS.Tile;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
public class AreaPicture {
  private String id;
  private String idUser;
  private String idProspect;
  private String idFileInfo;
  private String address;
  private double longitude;
  private double latitude;
  private Instant createdAt;
  private Instant updatedAt;
  private ZoomLevel zoomLevel;
  private MapLayer currentLayer;
  private Tile tile;
  private List<MapLayer> layers;

  public String getFilename() {
    return "%s_%s_%s_%s".formatted(currentLayer, zoomLevel.getValue(), tile.getX(), tile.getY());
  }
}
