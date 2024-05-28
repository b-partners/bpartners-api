package app.bpartners.api.service.WMS.imageSource;

import app.bpartners.api.file.FileDownloader;
import app.bpartners.api.model.AreaPictureMapLayer;
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
  public File downloadImage(String filename, Tile tile, AreaPictureMapLayer areaPictureMapLayer) {
    return fileDownloader.apply(filename, getURI(tile, areaPictureMapLayer));
  }

  protected abstract URI getURI(Tile tile, AreaPictureMapLayer areaPictureMapLayer);
}
