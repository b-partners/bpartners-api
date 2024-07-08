package app.bpartners.api.service.WMS.imageSource;

import app.bpartners.api.file.FileDownloader;
import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.service.WMS.Tile;
import java.net.URI;

abstract sealed class AbstractWmsImageSource implements WmsImageSource
    permits GeoserverImageSource, TileExtenderImageSource, WmsImageSourceFacade {
  protected final FileDownloader fileDownloaderImpl;

  protected AbstractWmsImageSource(FileDownloader fileDownloader) {
    this.fileDownloaderImpl = fileDownloader;
  }

  protected abstract URI getURI(Tile tile, AreaPictureMapLayer areaPictureMapLayer);
}
