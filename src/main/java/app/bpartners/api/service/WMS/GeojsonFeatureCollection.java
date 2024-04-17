package app.bpartners.api.service.WMS;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.springframework.core.io.ClassPathResource;

public enum GeojsonFeatureCollection {
  FRANCE_DEPARTEMENTS(readFranceDepartementsFeatureCollectionAsList());
  private final List<SimpleFeature> departements;

  GeojsonFeatureCollection(List<SimpleFeature> simpleFeatures) {
    this.departements = simpleFeatures;
  }

  public List<SimpleFeature> featureCollection() {
    return departements;
  }

  @SneakyThrows
  private static List<SimpleFeature> readFranceDepartementsFeatureCollectionAsList() {
    var classPathResource = new ClassPathResource("files/france-geojson/departements.geojson");
    InputStream inputStream = classPathResource.getInputStream();
    FeatureJSON featureJSON = new FeatureJSON();
    var simpleFeatureCollection =
        (SimpleFeatureCollection) featureJSON.readFeatureCollection(inputStream);
    List<SimpleFeature> res = new ArrayList<>();
    try (var features = simpleFeatureCollection.features()) {
      while (features.hasNext()) {
        res.add(features.next());
      }
    }
    return res;
  }
}
