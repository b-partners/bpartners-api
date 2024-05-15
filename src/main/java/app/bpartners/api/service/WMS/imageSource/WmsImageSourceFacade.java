package app.bpartners.api.service.WMS.imageSource;

import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.service.WMS.Tile;
import java.net.URI;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Primary
public class WmsImageSourceFacade implements WmsImageSource {
  private final OpenStreetMapImageSource openStreetMapImageSource;
  private final GeoserverImageSource geoserverImageSource;

  @Override
  public URI apply(Tile tile, AreaPictureMapLayer mapLayer) {
    return switch (mapLayer.getSource()) {
      case OPENSTREETMAP -> openStreetMapImageSource.apply(tile, mapLayer);
      case GEOSERVER -> geoserverImageSource.apply(tile, mapLayer);
    };
  }
}
