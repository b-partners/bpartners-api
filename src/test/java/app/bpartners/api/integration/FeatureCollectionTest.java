package app.bpartners.api.integration;

import static app.bpartners.api.service.wms.AreaPictureMapLayerService.WGS_84_SRID;
import static app.bpartners.api.service.wms.GeojsonFeatureCollection.getFranceAndQuebecDepartementsSimpleFeaturesMatchingPredicate;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.geotools.api.feature.simple.SimpleFeature;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

@Slf4j
public class FeatureCollectionTest {

  @Test
  void quebec_zone_is_valid() {
    var geometryFactory = new GeometryFactory(new PrecisionModel(), WGS_84_SRID);
    double longitude = -71.29137376844952;
    double latitude = 46.82801901634743;
    var areaPictureCoordinatesAsPoint =
        geometryFactory.createPoint(new Coordinate(longitude, latitude));

    List<SimpleFeature> features =
        getFranceAndQuebecDepartementsSimpleFeaturesMatchingPredicate(
            feature -> {
              var geometry = (Geometry) feature.getDefaultGeometry();
              return geometry.contains(areaPictureCoordinatesAsPoint);
            });

    List<String> matchingFeaturesName =
        features.stream().map(f -> (String) f.getAttribute("nom")).toList();

    assertTrue(matchingFeaturesName.contains("Quebec"));
  }

  @Test
  void all_departements_are_valid() {
    List<SimpleFeature> data =
        getFranceAndQuebecDepartementsSimpleFeaturesMatchingPredicate(f -> true);
    checkAllValidity(data);
  }

  private static void checkAllValidity(List<SimpleFeature> fds) {
    log.info("size {}", fds.size());
    List<String> valids = new ArrayList<>();
    StringBuilder notValids = new StringBuilder();
    fds.forEach(checkValidity(valids, notValids));
    if (!notValids.isEmpty()) {
      throw new RuntimeException(notValids.toString());
    }
  }

  @NotNull
  private static Consumer<SimpleFeature> checkValidity(
      List<String> valids, StringBuilder notValids) {
    return fd -> {
      var fdGeom = (Geometry) fd.getDefaultGeometry();
      String fdName = ((String) fd.getAttribute("nom")).toLowerCase();
      if (!fdGeom.isValid()) {
        notValids.append(fdName).append(" ");
      } else {
        valids.add(fdName);
      }
    };
  }
}
