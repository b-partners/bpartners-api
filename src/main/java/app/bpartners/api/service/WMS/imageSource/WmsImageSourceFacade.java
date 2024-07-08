package app.bpartners.api.service.WMS.imageSource;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.api.file.FileDownloader;
import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.service.WMS.AreaPictureMapLayerService;
import app.bpartners.api.service.WMS.Tile;
import app.bpartners.api.service.WMS.imageSource.exception.BlankImageException;
import java.io.File;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Range;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@Slf4j
final class WmsImageSourceFacade extends AbstractWmsImageSource {
  private final GeoserverImageSource geoserverImageSource;
  private final AreaPictureMapLayerService areaPictureMapLayerService;
  private final TileExtenderImageSource tileExtenderImageSource;
  private final ImageValidator imageValidator;

  private WmsImageSourceFacade(
      FileDownloader fileDownloader,
      GeoserverImageSource geoserverImageSource,
      AreaPictureMapLayerService areaPictureMapLayerService,
      TileExtenderImageSource tileExtenderImageSource,
      ImageValidator imageValidator) {
    super(fileDownloader);
    this.geoserverImageSource = geoserverImageSource;
    this.areaPictureMapLayerService = areaPictureMapLayerService;
    this.tileExtenderImageSource = tileExtenderImageSource;
    this.imageValidator = imageValidator;
  }

  @Override
  protected URI getURI(Tile tile, AreaPictureMapLayer areaPictureMapLayer) {
    return geoserverImageSource.getURI(tile, areaPictureMapLayer);
  }

  @Override
  public File downloadImage(AreaPicture areaPicture) {
    if (areaPicture.isExtended()) {
      return tileExtenderImageSource.downloadImage(areaPicture);
    }
    return cascadeRetryImageDownloadUntilValid(geoserverImageSource, areaPicture, 0);
  }

  private File cascadeRetryImageDownloadUntilValid(
      WmsImageSource wmsImageSource,
      AreaPicture areaPicture,
      @Range(from = 0, to = 1) int iteration) {
    WmsImageSource alternativeSource;
    AreaPictureMapLayer alternativeAreaPictureMapLayer;
    if (iteration == 0) {
      alternativeSource = wmsImageSource;
      alternativeAreaPictureMapLayer = areaPicture.getCurrentLayer();
      log.info("Current layer");
    } else if (iteration == 1) {
      alternativeSource = geoserverImageSource;
      alternativeAreaPictureMapLayer = areaPictureMapLayerService.getDefaultIGNLayer();
      log.info("Geoserver layer");
    } else {
      throw new ApiException(
          SERVER_EXCEPTION, "could not find any server for " + areaPicture.describe());
    }
    try {
      // imageValidator.accept(image);
      areaPicture.setCurrentLayer(alternativeAreaPictureMapLayer);
      return alternativeSource.downloadImage(areaPicture);
    } catch (ApiException | IllegalArgumentException | BlankImageException e) {
      log.info(
          "could not resolve {} , due to exception {}", areaPicture.describe(), e.getMessage());
      return cascadeRetryImageDownloadUntilValid(alternativeSource, areaPicture, ++iteration);
    }
  }

  @Override
  public boolean supports(AreaPicture areaPicture) {
    return true;
  }
}
