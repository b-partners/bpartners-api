package app.bpartners.api.service.WMS;

import static app.bpartners.api.service.WMS.GeojsonFeatureCollection.FRANCE_DEPARTEMENTS;
import static app.bpartners.api.service.WMS.MapLayer.TOUS_FR;

import app.bpartners.api.model.exception.BadRequestException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class MapLayerGuesser implements Function<Tile, List<MapLayer>> {
  public static final MapLayer DEFAULT_FRANCE_LAYER = TOUS_FR;
  public static final int WGS_84_SRID = 4326;

  @Override
  public List<MapLayer> apply(Tile tile) {
    var geometryFactory = new GeometryFactory(new PrecisionModel(), WGS_84_SRID);
    var areaPictureCoordinatesAsPoint =
        geometryFactory.createPoint(new Coordinate(tile.getLongitude(), tile.getLatitude()));
    List<SimpleFeature> features = FRANCE_DEPARTEMENTS.featureCollection();
    for (SimpleFeature feature : features) {
      var geometry = (Geometry) feature.getDefaultGeometry();
      if (geometry.contains(areaPictureCoordinatesAsPoint)) {
        String featureDepartementName = feature.getAttribute("nom").toString();
        var guessedLayers =
            Arrays.stream(MapLayer.values())
                .filter(
                    a -> StringUtils.containsIgnoreCase(a.getZoneName(), featureDepartementName))
                .toList();
        log.info(
            "feature.name {} was requested and we found {}", featureDepartementName, guessedLayers);
        return List.of(DEFAULT_FRANCE_LAYER);
      }
    }
    throw new BadRequestException(
        "no matching layer found for point " + areaPictureCoordinatesAsPoint);
  }

  public MapLayer getLatestOrDefault(List<MapLayer> layers) {
    return layers.stream()
        .max(Comparator.comparingInt(MapLayer::getYear))
        .orElse(DEFAULT_FRANCE_LAYER);
  }
}
