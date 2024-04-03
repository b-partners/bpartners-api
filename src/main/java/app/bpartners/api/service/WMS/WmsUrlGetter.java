package app.bpartners.api.service.WMS;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class WmsUrlGetter implements Function<Tile, URI> {
  private static final String URI_FORMAT =
      "https://wms.openstreetmap.fr/tms/1.0.0/%s/%d/%d/%d.jpeg";

  @Override
  public URI apply(Tile tile) {
    try {
      return new URI(
          String.format(
              URI_FORMAT,
              tile.getLayer().getValue(),
              tile.getArcgisZoom().getZoomLevel(),
              tile.getX(),
              tile.getY()));
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
