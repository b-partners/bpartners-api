package app.bpartners.api.service.WMS.imageSource;

import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.service.WMS.Tile;
import java.net.URI;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@AllArgsConstructor
final class OpenStreetMapImageSource extends AbstractWmsImageSource {
  private static final UriComponents BASE_URL_BUILDER =
      UriComponentsBuilder.fromUriString(
              "https://wms.openstreetmap.fr/tms/1.0.0/{layer}/{zoom}/{x}/{y}.jpeg")
          .build();

  @Override
  public URI getURI(Tile tile, AreaPictureMapLayer mapLayer) {
    Map<String, Object> uriVariables =
        Map.of(
            "layer",
            mapLayer.getName(),
            "zoom",
            tile.getArcgisZoom().getZoomLevel(),
            "x",
            tile.getX(),
            "y",
            tile.getY());
    return BASE_URL_BUILDER.expand(uriVariables).toUri();
  }
}
