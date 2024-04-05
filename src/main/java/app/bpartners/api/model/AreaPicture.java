package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.OpenStreetMapLayer;
import app.bpartners.api.endpoint.rest.model.ZoomLevel;
import app.bpartners.api.service.WMS.Tile;
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
  private String filename;
  private OpenStreetMapLayer layer;
  private Tile tile;
}
