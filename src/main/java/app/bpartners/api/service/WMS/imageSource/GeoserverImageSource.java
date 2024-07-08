package app.bpartners.api.service.WMS.imageSource;

import static app.bpartners.api.endpoint.rest.model.AreaPictureImageSource.GEOSERVER;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.api.file.FileDownloader;
import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.service.WMS.Tile;
import java.io.File;
import java.net.URI;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
final class GeoserverImageSource extends AbstractWmsImageSource {
  private final UriComponents baseUrl;

  public GeoserverImageSource(
      FileDownloader fileDownloader, @Value("${geoserver.baseurl}") String geoserverBaseUrl) {
    super(fileDownloader);
    this.baseUrl =
        UriComponentsBuilder.fromHttpUrl(geoserverBaseUrl)
            .query("layers={layer}")
            .query("zoom={zoom}")
            .query("x={x}")
            .query("y={y}")
            .query("format=image/jpeg")
            .build();
  }

  @Override
  public URI getURI(Tile tile, AreaPictureMapLayer mapLayer) {
    Map<String, Object> uriVariables =
        Map.of(
            "layer",
            mapLayer.getName(),
            "zoom",
            tile.getArcgisZoom().getZoomLevel(),
            "x",
            tile.getX(),
            "y",
            tile.getY());
    return baseUrl.expand(uriVariables).toUri();
  }

  @Override
  public File downloadImage(AreaPicture areaPicture) {
    if (!supports(areaPicture)) {
      throw new ApiException(
          SERVER_EXCEPTION,
          "cannot download " + areaPicture + " from " + this.getClass().getTypeName());
    }
    return fileDownloaderImpl.get(
        areaPicture.getFilename(),
        getURI(areaPicture.getCurrentTile(), areaPicture.getCurrentLayer()));
  }

  @Override
  public boolean supports(AreaPicture areaPicture) {
    return GEOSERVER.equals(areaPicture.getCurrentLayer().getSource());
  }
}
