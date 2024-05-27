package app.bpartners.api.service.WMS.imageSource;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.api.file.FileDownloader;
import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.service.WMS.Tile;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;

abstract sealed class AbstractWmsImageSource implements WmsImageSource
    permits GeoserverImageSource, OpenStreetMapImageSource, WmsImageSourceFacade {
  protected final FileDownloader fileDownloader;

  protected AbstractWmsImageSource() {
    this.fileDownloader = new FileDownloader(HttpClient.newHttpClient());
  }

  @Override
  public File downloadImage(AreaPicture areaPicture) {
    if (!supports(areaPicture)) {
      throw new ApiException(
          SERVER_EXCEPTION,
          "cannot download " + areaPicture.toString() + " from " + this.getClass().getTypeName());
    }
    return fileDownloader.apply(
        areaPicture.getFilename(), getURI(areaPicture.getTile(), areaPicture.getCurrentLayer()));
  }

  protected abstract URI getURI(Tile tile, AreaPictureMapLayer areaPictureMapLayer);
}
