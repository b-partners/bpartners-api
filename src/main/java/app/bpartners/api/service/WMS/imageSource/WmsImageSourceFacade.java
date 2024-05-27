package app.bpartners.api.service.WMS.imageSource;

import app.bpartners.api.model.AreaPicture;
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
  public File downloadImage(AreaPicture areaPicture) {
    return switch (areaPicture.getCurrentLayer().getSource()) {
      case OPENSTREETMAP -> openStreetMapImageSource.downloadImage(areaPicture);
      case GEOSERVER -> {
        var file = geoserverImageSource.downloadImage(areaPicture);
        try {
          imageValidator.accept(file);
          yield file;
        } catch (BlankImageException e) {
          file.delete();
          areaPicture.setCurrentLayer(areaPictureMapLayerService.getDefaultLayer());
          yield openStreetMapImageSource.downloadImage(areaPicture);
        }
      }
    };
  }

  @Override
  public boolean supports(AreaPicture areaPicture) {
    return true;
  }
}
