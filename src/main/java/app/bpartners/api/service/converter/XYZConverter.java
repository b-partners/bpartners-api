package app.bpartners.api.service.converter;

import app.bpartners.api.service.WMS.Tile;
import java.util.function.Function;

public interface XYZConverter extends Function<Tile, XYZToBoundingBox.BBOX> {}
