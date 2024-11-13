package app.bpartners.api.service.WMS.imageSource;

import static app.bpartners.api.service.WMS.Tile.from;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.service.WMS.ArcgisZoom;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@JsonAutoDetect(fieldVisibility = ANY)
@Data
@Builder
@Slf4j
public class TileExtenderRequestBody implements Serializable {
  public static final String OPENSTREETMAP_SERVER_NAME = "openstreetmap";
  public static final String GEOSERVER_SERVER_NAME = "geoserver";
  public static final String GEOSERVER_IGN_NAME = "geoserver_ign";
  public static final int DEFAULT_MAX_IGN_ZOOM = 19;
  private int x;
  private int y;
  private int z;
  private String server;
  private String layer;
  private Double latitude;
  private Double longitude;
  private boolean isCropped;
  private int shiftNb;

  private static String getSource(AreaPictureMapLayer areaPictureMapLayer) {
    return switch (areaPictureMapLayer.getSource()) {
      case OPENSTREETMAP -> OPENSTREETMAP_SERVER_NAME;
      case GEOSERVER -> GEOSERVER_SERVER_NAME;
      case GEOSERVER_IGN -> GEOSERVER_IGN_NAME;
    };
  }

  public static TileExtenderRequestBody fromAreaPicture(AreaPicture areaPicture) {
    double currentGeoPositionLongitude =
        areaPicture.getCurrentGeoPosition().getLongitude() != null
            ? areaPicture.getCurrentGeoPosition().getLongitude()
            : areaPicture.getCurrentTile().getLongitude();
    double currentGeoPositionLatitude =
        areaPicture.getCurrentGeoPosition().getLatitude() != null
            ? areaPicture.getCurrentGeoPosition().getLatitude()
            : areaPicture.getCurrentTile().getLatitude();
    int zoom = areaPicture.getArcgisZoom().getZoomLevel();
    var currentLayer = areaPicture.getCurrentLayer();
    String layer = currentLayer.getName();
    String server = getSource(currentLayer);
    log.info("Extended current layer={}", areaPicture.getCurrentLayer());
    if (GEOSERVER_IGN_NAME.equals(server) && areaPicture.getArcgisZoom().getZoomLevel() >= 20) {
      zoom = DEFAULT_MAX_IGN_ZOOM;
    }
    if (zoom == DEFAULT_MAX_IGN_ZOOM) {
      server = GEOSERVER_IGN_NAME;
      layer = "ORTHOIMAGERY.ORTHOPHOTOS";
    }
    var tile = from(currentGeoPositionLongitude, currentGeoPositionLatitude, ArcgisZoom.from(zoom));

    return TileExtenderRequestBody.builder()
        .x(tile.getX())
        .y(tile.getY())
        .z(zoom)
        .layer(layer)
        .server(server)
        .latitude(areaPicture.getCurrentTile().getLatitude())
        .longitude(areaPicture.getCurrentTile().getLongitude())
        .isCropped(areaPicture.isCropped())
        .shiftNb(areaPicture.getShiftNb())
        .build();
  }
}
