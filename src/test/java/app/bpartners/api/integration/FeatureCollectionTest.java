package app.bpartners.api.integration;

import static app.bpartners.api.service.WMS.GeojsonFeatureCollection.getFranceDepartementsSimpleFeaturesMatchingPredicate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.geotools.api.feature.simple.SimpleFeature;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;

@Slf4j
public class FeatureCollectionTest {
  @Test
  void all_departements_are_valid() {
    List<SimpleFeature> data = getFranceDepartementsSimpleFeaturesMatchingPredicate((f) -> true);
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
