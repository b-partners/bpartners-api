package app.bpartners.api.service.wms.imageSource;

import app.bpartners.api.file.FileDownloader;
import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.service.wms.Tile;
import java.net.URI;

abstract sealed class AbstractWmsImageSource implements WmsImageSource
    permits GeoserverImageSource,
        TileExtenderImageSource,
        IGNGeoserverImageSource,
        WmsImageSourceFacade {
  protected final FileDownloader fileDownloaderImpl;

  protected AbstractWmsImageSource(FileDownloader fileDownloader) {
    this.fileDownloaderImpl = fileDownloader;
  }

  protected abstract URI getURI(Tile tile, AreaPictureMapLayer areaPictureMapLayer);
}
