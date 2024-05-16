package app.bpartners.api.service.WMS.imageSource;

import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.service.WMS.AreaPictureMapLayerService;
import app.bpartners.api.service.WMS.Tile;
import app.bpartners.api.service.WMS.imageSource.exception.BlankImageException;
import java.io.File;
import java.net.URI;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Primary
final class WmsImageSourceFacade extends AbstractWmsImageSource {
  private final OpenStreetMapImageSource openStreetMapImageSource;
  private final GeoserverImageSource geoserverImageSource;
  private final AreaPictureMapLayerService areaPictureMapLayerService;
  private final ImageValidator imageValidator;

  @Override
  protected URI getURI(Tile tile, AreaPictureMapLayer areaPictureMapLayer) {
    return switch (areaPictureMapLayer.getSource()) {
      case OPENSTREETMAP -> openStreetMapImageSource.getURI(tile, areaPictureMapLayer);
      case GEOSERVER -> geoserverImageSource.getURI(tile, areaPictureMapLayer);
    };
  }

  @Override
  public File downloadImage(String filename, Tile tile, AreaPictureMapLayer areaPictureMapLayer) {
    return switch (areaPictureMapLayer.getSource()) {
      case OPENSTREETMAP -> openStreetMapImageSource.downloadImage(
          filename, tile, areaPictureMapLayer);
      case GEOSERVER -> {
        var file = geoserverImageSource.downloadImage(filename, tile, areaPictureMapLayer);
        try {
          imageValidator.accept(file);
          yield file;
        } catch (BlankImageException e) {
          file.delete();
          yield openStreetMapImageSource.downloadImage(
              filename, tile, areaPictureMapLayerService.getDefaultLayer());
        }
      }
    };
  }
}
