package app.bpartners.api.service.WMS.imageSource;

import static app.bpartners.api.endpoint.rest.model.AreaPictureImageSource.GEOSERVER_IGN;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.api.endpoint.rest.model.GeoPosition;
import app.bpartners.api.file.FileDownloader;
import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.validator.AreaPictureValidator;
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
  private final AreaPictureValidator areaPictureValidator;

  public IGNGeoserverImageSource(
      @Value("${ign.geoserver.baseurl}") String geoserverBaseUrl,
      FileDownloader fileDownloader,
      AreaPictureValidator areaPictureValidator) {
    super(fileDownloader);
    this.baseUrl =
        UriComponentsBuilder.fromHttpUrl(geoserverBaseUrl)
            .query("zoom={zoom}")
            .query("lat={lat}")
            .query("long={long}")
            .build();
    this.areaPictureValidator = areaPictureValidator;
  }

  @Override
  public File downloadImage(AreaPicture areaPicture) {
    if (!supports(areaPicture)) {
      throw new ApiException(
          SERVER_EXCEPTION,
          "cannot download " + areaPicture + " from " + this.getClass().getTypeName());
    }

    return fileDownloaderImpl.getFromS3(areaPicture.getFilename(), getURI(areaPicture));
  }

  @Override
  public boolean supports(AreaPicture areaPicture) {
    return GEOSERVER_IGN.equals(areaPicture.getCurrentLayer().getSource());
  }

  private URI getURI(AreaPicture areaPicture) {
    areaPictureValidator.accept(areaPicture);
    GeoPosition geoPosition = areaPicture.getCurrentGeoPosition();
    Double longitude = geoPosition.getLongitude();
    Double latitude = geoPosition.getLatitude();

    Map<String, Object> uriVariables =
        Map.of(
            "zoom", areaPicture.getArcgisZoom().getZoomLevel(),
            "lat", latitude,
            "long", longitude);

    return baseUrl.expand(uriVariables).toUri();
  }

  @Override
  protected URI getURI(Tile tile, AreaPictureMapLayer areaPictureMapLayer) {
    Map<String, Object> uriVariables =
        Map.of(
            "layer",
            areaPictureMapLayer.getName(),
            "zoom",
            tile.getArcgisZoom().getZoomLevel(),
            "x",
            tile.getX(),
            "y",
            tile.getY());
    return baseUrl.expand(uriVariables).toUri();
  }
}
