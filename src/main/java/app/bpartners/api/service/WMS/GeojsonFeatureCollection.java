package app.bpartners.api.service.WMS;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.SneakyThrows;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import org.springframework.core.io.ClassPathResource;

public final class GeojsonFeatureCollection {
  private GeojsonFeatureCollection() {}

  private static final List<SimpleFeature> FRANCE_DEPARTEMENTS_1_SFS =
      readFranceDepartementsFeatureCollectionAsList(1);
  private static final List<SimpleFeature> FRANCE_DEPARTEMENTS_2_SFS =
      readFranceDepartementsFeatureCollectionAsList(2);
  private static final List<SimpleFeature> FRANCE_DEPARTEMENTS_3_SFS =
      readFranceDepartementsFeatureCollectionAsList(3);
  private static final List<SimpleFeature> FRANCE_DEPARTEMENTS_4_SFS =
      readFranceDepartementsFeatureCollectionAsList(4);
  private static final List<SimpleFeature> FRANCE_DEPARTEMENTS_5_SFS =
      readFranceDepartementsFeatureCollectionAsList(5);
  private static final List<SimpleFeature> FRANCE_DEPARTEMENTS_6_SFS =
      readFranceDepartementsFeatureCollectionAsList(6);
  private static final List<SimpleFeature> FRANCE_DEPARTEMENTS_7_SFS =
      readFranceDepartementsFeatureCollectionAsList(7);
  private static final List<SimpleFeature> FRANCE_DEPARTEMENTS_8_SFS =
      readFranceDepartementsFeatureCollectionAsList(8);
  private static final List<SimpleFeature> FRANCE_DEPARTEMENTS_9_SFS =
      readFranceDepartementsFeatureCollectionAsList(9);
  private static final List<SimpleFeature> FRANCE_DEPARTEMENTS_10_SFS =
      readFranceDepartementsFeatureCollectionAsList(10);

  @SneakyThrows
  private static List<SimpleFeature> readFranceDepartementsFeatureCollectionAsList(
      @Range(from = 0, to = 10) int number) {
    return getSimpleFeatures("departements_%s.json".formatted(number));
  }

  public static List<SimpleFeature> getFranceDepartementsSimpleFeaturesMatchingPredicate(
      Predicate<SimpleFeature> predicate) {
    var result = new ArrayList<SimpleFeature>();
    var allLists =
        List.of(
            FRANCE_DEPARTEMENTS_1_SFS,
            FRANCE_DEPARTEMENTS_2_SFS,
            FRANCE_DEPARTEMENTS_3_SFS,
            FRANCE_DEPARTEMENTS_4_SFS,
            FRANCE_DEPARTEMENTS_5_SFS,
            FRANCE_DEPARTEMENTS_6_SFS,
            FRANCE_DEPARTEMENTS_7_SFS,
            FRANCE_DEPARTEMENTS_8_SFS,
            FRANCE_DEPARTEMENTS_9_SFS,
            FRANCE_DEPARTEMENTS_10_SFS);
    var matcherFunction = matchPredicate(predicate);
    allLists.forEach(list -> result.addAll(matcherFunction.apply(list)));
    return result;
  }

  private static Function<List<SimpleFeature>, List<SimpleFeature>> matchPredicate(
      Predicate<SimpleFeature> predicate) {
    return list -> list.stream().filter(predicate).toList();
  }

  @NotNull
  private static List<SimpleFeature> getSimpleFeatures(String geojsonFileName) throws IOException {
    var classPathResource = new ClassPathResource("files/france-geojson/" + geojsonFileName);
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
