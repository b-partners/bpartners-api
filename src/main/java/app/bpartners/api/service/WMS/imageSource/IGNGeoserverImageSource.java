package app.bpartners.api.service.WMS.imageSource;

import static app.bpartners.api.endpoint.rest.model.AreaPictureImageSource.GEOSERVER_IGN;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.api.endpoint.rest.model.GeoPosition;
import app.bpartners.api.file.FileDownloader;
import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.service.WMS.Tile;
import java.io.File;
import java.net.URI;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Component
final class IGNGeoserverImageSource extends AbstractWmsImageSource {
  private final UriComponents baseUrl;

  public IGNGeoserverImageSource(
      FileDownloader fileDownloader, @Value("${ign.geoserver.baseurl}") String geoserverBaseUrl) {
    super(fileDownloader);
    this.baseUrl =
        UriComponentsBuilder.fromHttpUrl(geoserverBaseUrl)
            .query("SERVICE=WMTS")
            .query("REQUEST=GetTile")
            .query("VERSION=1.0.0")
            .query("LAYER={layer}")
            .query("TILEMATRIXSET=PM")
            .query("TILEMATRIX={zoom}")
            .query("TILECOL={x}")
            .query("TILEROW={y}")
            .query("STYLE=normal")
            .query("format=image/jpeg")
            .build();
  }

  @Override
  protected URI getURI(Tile tile, AreaPictureMapLayer areaPictureMapLayer) {
    return null;
  }

  protected URI getURI(IgnGeopostion geoPosition, AreaPictureMapLayer mapLayer) {
    Map<String, Object> uriVariables =
        Map.of(
            "LAYER",
            mapLayer.getName(),
            "TILEMATRIX",
            mapLayer.getPrecisionLevelInCm(),
            "TILECOL",
            geoPosition.xTile,
            "TILEROW",
            geoPosition.yTile);
    return baseUrl.expand(uriVariables).toUri();
  }

  @Override
  public File downloadImage(AreaPicture areaPicture) {
    if (!supports(areaPicture)) {
      throw new ApiException(
          SERVER_EXCEPTION,
          "cannot download " + areaPicture + " from " + this.getClass().getTypeName());
    }
    IgnGeopostion ignGeopostion = coordinatesToIgnGeoposition(areaPicture);

    return fileDownloaderImpl.get(
        areaPicture.getFilename(), getURI(ignGeopostion, areaPicture.getCurrentLayer()));
  }

  @Override
  public boolean supports(AreaPicture areaPicture) {
    return GEOSERVER_IGN.equals(areaPicture.getCurrentLayer().getSource());
  }

  private record IgnGeopostion(Integer xTile, Integer yTile) {}

  private IgnGeopostion coordinatesToIgnGeoposition(AreaPicture areaPicture) {
    GeoPosition geoPosition = areaPicture.getCurrentGeoPosition();
    double n = Math.pow(2, areaPicture.getArcgisZoom().getZoomLevel());
    double xTile = n * ((geoPosition.getLongitude() + 180) / 360);
    double latRad = Math.toRadians(geoPosition.getLatitude());
    double yTile = n * (1 - (Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI)) / 2;
    return new IgnGeopostion((int) xTile, (int) yTile);
  }
}
