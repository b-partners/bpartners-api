package app.bpartners.api.service.WMS.imageSource;

import static app.bpartners.api.endpoint.rest.model.AreaPictureImageSource.OPENSTREETMAP;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.api.file.FileDownloader;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.service.WMS.Tile;
import java.io.File;
import java.net.URI;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Component
final class OpenStreetMapImageSource extends AbstractWmsImageSource {
  private static final UriComponents BASE_URL_BUILDER =
      UriComponentsBuilder.fromUriString(
              "https://wms.openstreetmap.fr/tms/1.0.0/{layer}/{zoom}/{x}/{y}.jpeg")
          .build();

  private OpenStreetMapImageSource(FileDownloader fileDownloader) {
    super(fileDownloader);
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
    return BASE_URL_BUILDER.expand(uriVariables).toUri();
  }

  @Override
  public File downloadImage(AreaPicture areaPicture, AccountHolder accountHolder) {
    return null;
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
    return OPENSTREETMAP.equals(areaPicture.getCurrentLayer().getSource());
  }
}
