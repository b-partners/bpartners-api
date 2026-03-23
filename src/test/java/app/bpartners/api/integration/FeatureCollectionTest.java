package app.bpartners.api.integration;

import static app.bpartners.api.service.wms.AreaPictureMapLayerService.WGS_84_SRID;
import static app.bpartners.api.service.wms.GeojsonFeatureCollection.getFranceAndQuebecDepartementsSimpleFeaturesMatchingPredicate;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.geotools.api.feature.simple.SimpleFeature;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

@Slf4j
public class FeatureCollectionTest {
  @ParameterizedTest
  @MethodSource("zoneTestCases")
  void zone_is_valid(ZoneTestCase testCase) {
    var geometryFactory = new GeometryFactory(new PrecisionModel(), WGS_84_SRID);
    var point =
        geometryFactory.createPoint(new Coordinate(testCase.longitude(), testCase.latitude()));

    List<String> matchingFeaturesName =
        getFranceAndQuebecDepartementsSimpleFeaturesMatchingPredicate(
                feature -> ((Geometry) feature.getDefaultGeometry()).contains(point))
            .stream()
            .map(f -> (String) f.getAttribute("nom"))
            .toList();

    assertTrue(matchingFeaturesName.contains(testCase.expectedName()));
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

  record ZoneTestCase(double longitude, double latitude, String expectedName) {}

  static Stream<Arguments> zoneTestCases() {
    return Stream.of(
        Arguments.of(new ZoneTestCase(2.3935203, 51.05007060000001, "Nord")),
        Arguments.of(new ZoneTestCase(6.111297178735165, 49.744293262381476, "Luxembourg")),
        Arguments.of(new ZoneTestCase(-71.29137376844952, 46.82801901634743, "Quebec")),
        Arguments.of(new ZoneTestCase(6.2242825, 46.2193426, "Suisse")));
  }
}
