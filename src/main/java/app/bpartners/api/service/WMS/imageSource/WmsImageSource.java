package app.bpartners.api.service.WMS.imageSource;

import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.service.WMS.Tile;
import java.io.File;

public interface WmsImageSource {
  File downloadImage(String filename, Tile tile, AreaPictureMapLayer areaPictureMapLayer);
}
