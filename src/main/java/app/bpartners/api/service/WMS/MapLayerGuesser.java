package app.bpartners.api.service.WMS;

import static app.bpartners.api.service.WMS.GeojsonFeatureCollection.getFranceDepartementsSimpleFeaturesMatchingPredicate;
import static app.bpartners.api.service.WMS.MapLayer.TOUS_FR;

import app.bpartners.api.model.AreaPicture;
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
public class MapLayerGuesser implements Function<AreaPicture, List<MapLayer>> {
  public static final MapLayer DEFAULT_FRANCE_LAYER = TOUS_FR;
  public static final int WGS_84_SRID = 4326;

  @Override
  public List<MapLayer> apply(AreaPicture areaPicture) {
    var geometryFactory = new GeometryFactory(new PrecisionModel(), WGS_84_SRID);
    var areaPictureCoordinatesAsPoint =
        geometryFactory.createPoint(
            new Coordinate(areaPicture.getLongitude(), areaPicture.getLatitude()));
    List<SimpleFeature> features =
        getFranceDepartementsSimpleFeaturesMatchingPredicate(
            feature -> {
              var geometry = (Geometry) feature.getDefaultGeometry();
              return geometry.contains(areaPictureCoordinatesAsPoint);
            });
    for (SimpleFeature feature : features) {
      String featureDepartementName = feature.getAttribute("nom").toString();
      var guessedLayers =
          Arrays.stream(MapLayer.values())
              .filter(a -> StringUtils.containsIgnoreCase(a.getZoneName(), featureDepartementName))
              .toList();
      log.info(
          "feature.name {} was requested and we found {}", featureDepartementName, guessedLayers);
      return List.of(DEFAULT_FRANCE_LAYER);
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
