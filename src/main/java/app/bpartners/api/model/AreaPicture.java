package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.ZoomLevel;
import app.bpartners.api.service.WMS.ArcgisZoom;
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
  private static final String NORMAL_AREA_PICTURE_FILENAME_FORMAT = "%s_%s_%s_%s";
  private static final String EXTENDED_AREA_PICTURE_FILENAME_FORMAT = "%s_%s_%s_%s_%s";
  private static final String EXTENDED_KEYWORD = "extended";
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
  private AreaPictureMapLayer currentLayer;
  private Tile tile;
  private List<AreaPictureMapLayer> layers;
  private boolean isExtended;

  public String getFilename() {
    return isExtended
        ? getExtendedAreaPictureFilenameFormat()
        : getNormalAreaPictureFilenameFormat();
  }

  private String getNormalAreaPictureFilenameFormat() {
    return NORMAL_AREA_PICTURE_FILENAME_FORMAT.formatted(
        currentLayer.getName(), zoomLevel.getValue(), tile.getX(), tile.getY());
  }

  private String getExtendedAreaPictureFilenameFormat() {
    return EXTENDED_AREA_PICTURE_FILENAME_FORMAT.formatted(
        currentLayer.getName(), zoomLevel.getValue(), tile.getX(), tile.getY(), EXTENDED_KEYWORD);
  }

  public ArcgisZoom getArcgisZoom() {
    return ArcgisZoom.from(this.zoomLevel);
  }
}
