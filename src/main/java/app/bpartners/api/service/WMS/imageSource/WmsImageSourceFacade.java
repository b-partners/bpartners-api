package app.bpartners.api.service.WMS.imageSource;

import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.service.WMS.AreaPictureMapLayerService;
import app.bpartners.api.service.WMS.Tile;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import javax.imageio.ImageIO;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Primary
final class WmsImageSourceFacade extends AbstractWmsImageSource {
  private final OpenStreetMapImageSource openStreetMapImageSource;
  private final GeoserverImageSource geoserverImageSource;
  private final AreaPictureMapLayerService areaPictureMapLayerService;

  @Override
  protected URI getURI(Tile tile, AreaPictureMapLayer areaPictureMapLayer) {
    return switch (areaPictureMapLayer.getSource()) {
      case OPENSTREETMAP -> openStreetMapImageSource.getURI(tile, areaPictureMapLayer);
      case GEOSERVER -> geoserverImageSource.getURI(tile, areaPictureMapLayer);
    };
  }

  @Override
  public File downloadImage(String filename, Tile tile, AreaPictureMapLayer areaPictureMapLayer) {
    var uri = getURI(tile, areaPictureMapLayer);
    return switch (areaPictureMapLayer.getSource()) {
      case OPENSTREETMAP -> fileDownloader.apply(filename, uri);
      case GEOSERVER -> {
        var file = fileDownloader.apply(filename, uri);
        if (checkImageValidityAsFile(file)) {
          yield file;
        } else {
          file.delete();
          yield fileDownloader.apply(
              filename, getURI(tile, areaPictureMapLayerService.getDefaultLayer()));
        }
      }
    };
  }

  @SneakyThrows
  public static boolean checkImageValidityAsFile(File file) {
    BufferedImage bufferedImage = ImageIO.read(file);
    return true;
  }
}
