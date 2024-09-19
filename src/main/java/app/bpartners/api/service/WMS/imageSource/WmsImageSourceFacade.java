package app.bpartners.api.service.WMS.imageSource;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.api.file.FileDownloader;
import app.bpartners.api.mail.Mailer;
import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.service.WMS.AreaPictureMapLayerService;
import app.bpartners.api.service.WMS.Tile;
import java.io.File;
import java.net.URI;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@Slf4j
final class WmsImageSourceFacade extends AbstractWmsImageSource {
  private final GeoserverImageSource geoserverImageSource;
  private final IGNGeoserverImageSource ignGeoserverImageSource;
  private final AreaPictureMapLayerService areaPictureMapLayerService;
  private final TileExtenderImageSource tileExtenderImageSource;
  private final ImageValidator imageValidator;
  private final Mailer mailer;

  private WmsImageSourceFacade(
      FileDownloader fileDownloader,
      GeoserverImageSource geoserverImageSource,
      IGNGeoserverImageSource ignGeoserverImageSource,
      AreaPictureMapLayerService areaPictureMapLayerService,
      TileExtenderImageSource tileExtenderImageSource,
      ImageValidator imageValidator,
      Mailer mailer) {
    super(fileDownloader);
    this.geoserverImageSource = geoserverImageSource;
    this.ignGeoserverImageSource = ignGeoserverImageSource;
    this.areaPictureMapLayerService = areaPictureMapLayerService;
    this.tileExtenderImageSource = tileExtenderImageSource;
    this.imageValidator = imageValidator;
    this.mailer = mailer;
  }

  @Override
  protected URI getURI(Tile tile, AreaPictureMapLayer areaPictureMapLayer) {
    return geoserverImageSource.getURI(tile, areaPictureMapLayer);
  }

  @Override
  @SneakyThrows
  public File downloadImage(AreaPicture areaPicture) {
    if (areaPicture.isExtended()) {
      return tileExtenderImageSource.downloadImage(areaPicture);
    }
    return cascadeRetryImageDownloadUntilValid(geoserverImageSource, areaPicture);
  }

  private File cascadeRetryImageDownloadUntilValid(
      WmsImageSource wmsImageSource, AreaPicture areaPicture) {
    try {
      return getImage(wmsImageSource, areaPicture);
    } catch (RuntimeException e) {
      log.error("Error={} when trying to get areaPicture={}", e, areaPicture.describe());
      return tryOtherLayers(wmsImageSource, areaPicture);
    }
  }

  private File tryOtherLayers(WmsImageSource source, AreaPicture areaPicture) {
    var IGNLayer = areaPictureMapLayerService.getDefaultIGNLayer();
    var layers = areaPicture.getLayers();
    layers.addLast(IGNLayer);
    for (AreaPictureMapLayer layer : layers) {
      areaPicture.setCurrentLayer(layer);
      if (layer.equals(IGNLayer)) {
        source = ignGeoserverImageSource;
      }
      var image = getImage(source, areaPicture);
      if (image.exists()) {
        return image;
      }
    }
    throw new ApiException(
        SERVER_EXCEPTION, "could not find any server for " + areaPicture.describe());
  }

  private File getImage(WmsImageSource source, AreaPicture areaPicture) {
    var image = source.downloadImage(areaPicture);
    imageValidator.accept(image);
    return image;
  }

  @Override
  public boolean supports(AreaPicture areaPicture) {
    return true;
  }
}
