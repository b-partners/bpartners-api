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
  private final OpenStreetMapImageSource openStreetMapImageSource;
  private final GeoserverImageSource geoserverImageSource;
  private final AreaPictureMapLayerService areaPictureMapLayerService;
  private final TileExtenderImageSource tileExtenderImageSource;
  private final ImageValidator imageValidator;

  private WmsImageSourceFacade(
      FileDownloader fileDownloader,
      OpenStreetMapImageSource openStreetMapImageSource,
      GeoserverImageSource geoserverImageSource,
      AreaPictureMapLayerService areaPictureMapLayerService,
      TileExtenderImageSource tileExtenderImageSource,
      ImageValidator imageValidator) {
    super(fileDownloader);
    this.openStreetMapImageSource = openStreetMapImageSource;
    this.geoserverImageSource = geoserverImageSource;
    this.areaPictureMapLayerService = areaPictureMapLayerService;
    this.tileExtenderImageSource = tileExtenderImageSource;
    this.imageValidator = imageValidator;
  }

  @Override
  protected URI getURI(Tile tile, AreaPictureMapLayer areaPictureMapLayer) {
    return switch (areaPictureMapLayer.getSource()) {
      case OPENSTREETMAP -> openStreetMapImageSource.getURI(tile, areaPictureMapLayer);
      case GEOSERVER -> geoserverImageSource.getURI(tile, areaPictureMapLayer);
    };
  }

  @Override
  public File downloadImage(AreaPicture areaPicture) {
    if (areaPicture.isExtended()) {
      return tileExtenderImageSource.downloadImage(areaPicture);
    }
    return switch (areaPicture.getCurrentLayer().getSource()) {
      case OPENSTREETMAP -> openStreetMapImageSource.downloadImage(areaPicture);
      case GEOSERVER -> cascadeRetryImageDownloadUntilValid(geoserverImageSource, areaPicture, 0);
    };
  }

  private File cascadeRetryImageDownloadUntilValid(
      WmsImageSource wmsImageSource,
      AreaPicture areaPicture,
      @Range(from = 0, to = 3) int iteration) {
    WmsImageSource alternativeSource;
    AreaPictureMapLayer alternativeAreaPictureMapLayer;
    if (iteration == 0) {
      alternativeSource = wmsImageSource;
      alternativeAreaPictureMapLayer = areaPicture.getCurrentLayer();
    } else if (iteration == 1) {
      alternativeSource = geoserverImageSource;
      alternativeAreaPictureMapLayer = areaPictureMapLayerService.getDefaultIGNLayer();
    } else if (iteration == 2) {
      alternativeSource = openStreetMapImageSource;
      alternativeAreaPictureMapLayer = areaPictureMapLayerService.getDefaultOSMLayer();
    } else {
      throw new ApiException(
          SERVER_EXCEPTION, "could not find any server for " + areaPicture.describe());
    }
    try {
      // imageValidator.accept(image);
      return alternativeSource.downloadImage(areaPicture);
    } catch (ApiException | BlankImageException e) {
      log.info(
          "could not resolve {} , due to exception {}", areaPicture.describe(), e.getMessage());
      areaPicture.setCurrentLayer(alternativeAreaPictureMapLayer);
      return cascadeRetryImageDownloadUntilValid(alternativeSource, areaPicture, ++iteration);
    }
  }

  @Override
  public boolean supports(AreaPicture areaPicture) {
    return true;
  }
}
