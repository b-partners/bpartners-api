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
  private final IGNGeoserverImageSource ignGeoserverImageSource;
  private final AreaPictureMapLayerService areaPictureMapLayerService;
  private final TileExtenderImageSource tileExtenderImageSource;
  private final ImageValidator imageValidator;
  private final String FLUX_IGN_GEOSERVER_ID = "9a4bd8b7-556b-49a1-bea0-c35e961dab64";

  private WmsImageSourceFacade(
      FileDownloader fileDownloader,
      GeoserverImageSource geoserverImageSource,
      IGNGeoserverImageSource ignGeoserverImageSource,
      AreaPictureMapLayerService areaPictureMapLayerService,
      TileExtenderImageSource tileExtenderImageSource,
      ImageValidator imageValidator) {
    super(fileDownloader);
    this.geoserverImageSource = geoserverImageSource;
    this.ignGeoserverImageSource = ignGeoserverImageSource;
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
      @Range(from = 0, to = 3) int iteration) {
    WmsImageSource alternativeSource;
    AreaPictureMapLayer alternativeAreaPictureMapLayer;
    if (iteration == 0) {
      alternativeSource = wmsImageSource;
      alternativeAreaPictureMapLayer = areaPicture.getCurrentLayer();
    } else if (iteration == 1) {
      alternativeSource = geoserverImageSource;
      alternativeAreaPictureMapLayer = areaPictureMapLayerService.getById(FLUX_IGN_GEOSERVER_ID);
    } else if (iteration == 2) {
      alternativeSource = ignGeoserverImageSource;
      alternativeAreaPictureMapLayer = areaPictureMapLayerService.getDefaultIGNLayer();
    } else {
      throw new ApiException(
          SERVER_EXCEPTION, "could not find any server for " + areaPicture.describe());
    }
    try {
      areaPicture.setCurrentLayer(alternativeAreaPictureMapLayer);
      var image = alternativeSource.downloadImage(areaPicture);
//      imageValidator.accept(image);
      return image;
    } catch (ApiException | BlankImageException e) {
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
