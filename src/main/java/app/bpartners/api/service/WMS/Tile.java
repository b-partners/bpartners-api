package app.bpartners.api.service.WMS;

import static java.lang.Math.PI;

import app.bpartners.api.endpoint.rest.model.GeoPosition;
import app.bpartners.api.model.AreaPicture;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class Tile {
  private final int x;
  private final int y;
  private ArcgisZoom arcgisZoom;

  private static int getxTile(double longitude, int zoom) {
    int xtile = (int) Math.floor((longitude + 180) / 360 * (1 << zoom));
    if (xtile < 0) {
      return 0;
    }
    if (xtile >= (1 << zoom)) {
      return ((1 << zoom) - 1);
    }
    return xtile;
  }

  private static int getyTile(double latitude, int zoom) {
    int ytile =
        (int)
            Math.floor(
                (1
                        - Math.log(
                                Math.tan(Math.toRadians(latitude))
                                    + 1 / Math.cos(Math.toRadians(latitude)))
                            / PI)
                    / 2
                    * (1 << zoom));
    if (ytile < 0) {
      return 0;
    }
    if (ytile >= (1 << zoom)) {
      return ((1 << zoom) - 1);
    }
    return ytile;
  }

  public static Tile from(double longitude, double latitude, ArcgisZoom arcgisZoom) {
    int xTile = getxTile(longitude, arcgisZoom.getZoomLevel());
    int yTile = getyTile(latitude, arcgisZoom.getZoomLevel());
    return Tile.builder().x(xTile).y(yTile).arcgisZoom(arcgisZoom).build();
  }

  public static Tile from(AreaPicture areaPicture) {
    GeoPosition currentGeoPosition = areaPicture.getCurrentGeoPosition();
    return from(
        currentGeoPosition.getLongitude(),
        currentGeoPosition.getLatitude(),
        ArcgisZoom.from(areaPicture.getZoomLevel()));
  }

  public double getLongitude() {
    return x / Math.pow(2.0, arcgisZoom.getZoomLevel()) * 360.0 - 180;
  }

  public double getLatitude() {
    double n = PI - (2.0 * PI * y) / Math.pow(2.0, arcgisZoom.getZoomLevel());
    return Math.toDegrees(Math.atan(Math.sinh(n)));
  }

  public Tile getTopLeftTile() {
    return new Tile(x - 1, y - 1, arcgisZoom);
  }
}
