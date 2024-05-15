package app.bpartners.api.service.WMS.imageSource;

import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.service.WMS.Tile;
import java.net.URI;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class GeoserverImageSource implements WmsImageSource {
  private final UriComponents baseUrl;

  public GeoserverImageSource(@Value("${geoserver.baseurl}") String geoserverBaseUrl) {
    this.baseUrl =
        UriComponentsBuilder.fromHttpUrl(geoserverBaseUrl)
            .query("layers={layer}")
            .query("zoom={zoom}")
            .query("x={x}")
            .query("y={y}")
            .query("format=image/jpeg")
            .build();
  }

  @Override
  public URI apply(Tile tile, AreaPictureMapLayer mapLayer) {
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
    return baseUrl.expand(uriVariables).toUri();
  }
}
