package app.bpartners.api.service.WMS.imageSource;

import static app.bpartners.api.endpoint.rest.model.AreaPictureImageSource.GEOSERVER;

import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.service.WMS.Tile;
import java.net.URI;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Component
final class TileExtenderImageSource extends AbstractWmsImageSource {
  private final UriComponents baseUrl;

  public TileExtenderImageSource(@Value("${tile.extender.baseurl}") String tileExtenderBaseUrl) {
    this.baseUrl =
        UriComponentsBuilder.fromHttpUrl(tileExtenderBaseUrl)
            .query("layers={layer}")
            .query("zoom={zoom}")
            .query("x={x}")
            .query("y={y}")
            .query("format=image/jpeg")
            .build();
  }

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
    return baseUrl.expand(uriVariables).toUri();
  }

  @Override
  public boolean supports(AreaPicture areaPicture) {
    return GEOSERVER.equals(areaPicture.getCurrentLayer().getSource());
  }
}
