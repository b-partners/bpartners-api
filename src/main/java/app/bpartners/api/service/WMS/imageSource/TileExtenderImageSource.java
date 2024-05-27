package app.bpartners.api.service.WMS.imageSource;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

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
final class TileExtenderImageSource extends AbstractWmsImageSource {
  private final UriComponents baseUrl;

  public TileExtenderImageSource(
      FileDownloader fileDownloader,
      @Value("${tile.extender.baseurl}") String tileExtenderBaseUrl) {
    super(fileDownloader);
    this.baseUrl =
        UriComponentsBuilder.fromHttpUrl(tileExtenderBaseUrl)
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
    boolean isBase64Encoded = true;
    return fileDownloaderImpl.postJson(
        areaPicture.getFilename(),
        getURI(areaPicture.getTile(), areaPicture.getCurrentLayer()),
        null,
        isBase64Encoded);
  }

  @Override
  public boolean supports(AreaPicture areaPicture) {
    return areaPicture.isExtended();
  }
}
