package app.bpartners.api.service.WMS;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.BiFunction;
import org.springframework.stereotype.Component;

@Component
public class WmsUrlGetter implements BiFunction<Tile, MapLayer, URI> {
  private static final String URI_FORMAT =
      "https://wms.openstreetmap.fr/tms/1.0.0/%s/%d/%d/%d.jpeg";

  @Override
  public URI apply(Tile tile, MapLayer mapLayer) {
    try {
      return new URI(
          String.format(
              URI_FORMAT,
              mapLayer.getValue(),
              tile.getArcgisZoom().getZoomLevel(),
              tile.getX(),
              tile.getY()));
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
