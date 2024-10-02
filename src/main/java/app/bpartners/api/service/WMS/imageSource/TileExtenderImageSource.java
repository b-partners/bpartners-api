package app.bpartners.api.service.WMS.imageSource;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.service.WMS.Tile.from;

import app.bpartners.api.file.FileDownloader;
import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.service.WMS.Tile;
import java.io.File;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Component
final class TileExtenderImageSource extends AbstractWmsImageSource {
  private final UriComponents baseUrl;

  public TileExtenderImageSource(
      FileDownloader fileDownloader,
      @Value("${tile.extender.baseurl}") String tileExtenderBaseUrl) {
    super(fileDownloader);
    this.baseUrl = UriComponentsBuilder.fromHttpUrl(tileExtenderBaseUrl).path("/extend").build();
  }

  @Override
  public URI getURI(Tile tile, AreaPictureMapLayer mapLayer) {
    return baseUrl.toUri();
  }

  @Override
  public File downloadImage(AreaPicture areaPicture) {
    if (!supports(areaPicture)) {
      throw new ApiException(
          SERVER_EXCEPTION,
          "cannot download " + areaPicture + " from " + this.getClass().getTypeName());
    }
    boolean isBase64Encoded = true;
    double currentGeoPositionLongitude =
        areaPicture.getCurrentGeoPosition().getLongitude() != null
            ? areaPicture.getCurrentGeoPosition().getLongitude()
            : areaPicture.getCurrentTile().getLongitude();
    double currentGeoPositionLatitude =
        areaPicture.getCurrentGeoPosition().getLatitude() != null
            ? areaPicture.getCurrentGeoPosition().getLatitude()
            : areaPicture.getCurrentTile().getLatitude();
    return fileDownloaderImpl.postJson(
        areaPicture.getFilename(),
        getURI(
            from(
                currentGeoPositionLongitude,
                currentGeoPositionLatitude,
                areaPicture.getArcgisZoom()),
            areaPicture.getCurrentLayer()),
        TileExtenderRequestBody.fromAreaPicture(areaPicture),
        isBase64Encoded);
  }

  @Override
  public boolean supports(AreaPicture areaPicture) {
    return areaPicture.isExtended();
  }
}
