package app.bpartners.api.service.wms.imageSource;

import static app.bpartners.api.endpoint.rest.model.AreaPictureImageSource.AIRBUS;
import static app.bpartners.api.endpoint.rest.model.ZoomLevel.BUILDING;
import static app.bpartners.api.endpoint.rest.model.ZoomLevel.HOUSES_0;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.api.file.FileDownloader;
import app.bpartners.api.mail.Mailer;
import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.service.wms.AreaPictureMapLayerService;
import app.bpartners.api.service.wms.Tile;
import java.io.File;
import java.net.URI;
import java.util.Comparator;
import lombok.SneakyThrows;
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
  private final Mailer mailer;

  private WmsImageSourceFacade(
      FileDownloader fileDownloader,
      GeoserverImageSource geoserverImageSource,
      IGNGeoserverImageSource ignGeoserverImageSource,
      AreaPictureMapLayerService areaPictureMapLayerService,
      TileExtenderImageSource tileExtenderImageSource,
      Mailer mailer) {
    super(fileDownloader);
    this.geoserverImageSource = geoserverImageSource;
    this.ignGeoserverImageSource = ignGeoserverImageSource;
    this.areaPictureMapLayerService = areaPictureMapLayerService;
    this.tileExtenderImageSource = tileExtenderImageSource;
    this.mailer = mailer;
  }

  @Override
  protected URI getURI(Tile tile, AreaPictureMapLayer areaPictureMapLayer) {
    return geoserverImageSource.getURI(tile, areaPictureMapLayer);
  }

  @Override
  @SneakyThrows
  public File downloadImage(AreaPicture areaPicture) {
    return cascadeRetryImageDownloadUntilValid(geoserverImageSource, areaPicture, 0);
  }

  private File cascadeRetryImageDownloadUntilValid(
      WmsImageSource wmsImageSource,
      AreaPicture areaPicture,
      @Range(from = 0, to = Integer.MAX_VALUE) int iteration) {
    var orderedLayers =
        areaPictureMapLayerService
            .getAvailableLayersFrom(areaPicture.getCurrentGeoPosition())
            .stream()
            .sorted(
                Comparator.comparingInt(AreaPictureMapLayer::getPrecisionLevelInCm)
                    .thenComparing(
                        Comparator.comparingInt(AreaPictureMapLayer::getYear).reversed()))
            .toList();

    if (iteration < orderedLayers.size()) {
      areaPicture.setCurrentLayer(orderedLayers.get(iteration));

      if ("IGN_PHOTO_AERIENNE".equals(areaPicture.getCurrentLayer().getName())
          && areaPicture.getZoomLevel().equals(HOUSES_0)) {
        areaPicture.setZoomLevel(BUILDING);
      }
    } else {
      switch (iteration - orderedLayers.size()) {
        case 0 -> areaPicture.setCurrentLayer(areaPictureMapLayerService.getPCRSLayer());
        case 1 -> areaPicture.setCurrentLayer(areaPictureMapLayerService.getRhonePCRSLayer());
        case 2 -> {
          areaPicture.setCurrentLayer(areaPictureMapLayerService.getDefaultIGNLayer());
          setMaxZoomLevel(areaPicture);
        }
        case 3 -> {
          areaPicture.setCurrentLayer(areaPictureMapLayerService.getAirbusLayer());
          setMaxZoomLevel(areaPicture);
        }
        default ->
            throw new ApiException(
                SERVER_EXCEPTION, "could not find any server for " + areaPicture.describe());
      }
    }

    try {
      log.info("Process image download from layer = {}", areaPicture.getCurrentLayer());
      return tileExtenderImageSource.downloadImage(areaPicture);
    } catch (ApiException e) {
      log.info(
          "could not resolve {} , due to exception {}", areaPicture.describe(), e.getMessage());

      if (AIRBUS.equals(areaPicture.getCurrentLayer().getSource()) && iteration == 0) {
        throw new ApiException(SERVER_EXCEPTION, "PNEO data is not available yet on this area");
      }

      return cascadeRetryImageDownloadUntilValid(
          tileExtenderImageSource, areaPicture, iteration + 1);
    }
  }

  private void setMaxZoomLevel(AreaPicture areaPicture) {
    if (areaPicture.getArcgisZoom().getZoomLevel() >= 20) {
      areaPicture.setZoomLevel(BUILDING);
    }
  }

  @Override
  public boolean supports(AreaPicture areaPicture) {
    return true;
  }
}
