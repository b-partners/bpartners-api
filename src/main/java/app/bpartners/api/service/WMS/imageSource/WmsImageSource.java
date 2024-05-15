package app.bpartners.api.service.WMS.imageSource;

import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.service.WMS.Tile;
import java.net.URI;
import java.util.function.BiFunction;

public interface WmsImageSource extends BiFunction<Tile, AreaPictureMapLayer, URI> {}
