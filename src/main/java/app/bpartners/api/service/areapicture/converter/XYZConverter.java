package app.bpartners.api.service.areapicture.converter;

import app.bpartners.api.service.wms.Tile;
import java.util.function.Function;

public interface XYZConverter extends Function<Tile, XYZToBoundingBox.BBOX> {}
