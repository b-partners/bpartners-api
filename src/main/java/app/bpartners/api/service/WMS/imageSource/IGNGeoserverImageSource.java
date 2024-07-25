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
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Component
final class IGNGeoserverImageSource extends AbstractWmsImageSource {
  private final UriComponents baseUrl;

  public IGNGeoserverImageSource(
      @Value("${ign.geoserver.baseurl}") String geoserverBaseUrl, FileDownloader fileDownloader) {
    super(fileDownloader);
    this.baseUrl =
        UriComponentsBuilder.fromHttpUrl(geoserverBaseUrl)
            .query("zoom={zoom}")
            .query("lat={lat}")
            .query("long={long}")
            .build();
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
    GeoPosition geoPosition = areaPicture.getCurrentGeoPosition();
    Double longitude = geoPosition.getLongitude();
    Double latitude = geoPosition.getLatitude();

    Objects.requireNonNull(latitude, "Latitude cannot be null");
    Objects.requireNonNull(longitude, "Longitude cannot be null");

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
