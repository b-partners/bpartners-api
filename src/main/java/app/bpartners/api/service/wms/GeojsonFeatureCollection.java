package app.bpartners.api.service.wms;

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
      readEuropeFeatureCollectionAsList(1);
  private static final List<SimpleFeature> FRANCE_DEPARTEMENTS_2_SFS =
      readEuropeFeatureCollectionAsList(2);
  private static final List<SimpleFeature> FRANCE_DEPARTEMENTS_3_SFS =
      readEuropeFeatureCollectionAsList(3);
  private static final List<SimpleFeature> FRANCE_DEPARTEMENTS_4_SFS =
      readEuropeFeatureCollectionAsList(4);
  private static final List<SimpleFeature> FRANCE_DEPARTEMENTS_5_SFS =
      readEuropeFeatureCollectionAsList(5);
  private static final List<SimpleFeature> FRANCE_DEPARTEMENTS_6_SFS =
      readEuropeFeatureCollectionAsList(6);
  private static final List<SimpleFeature> FRANCE_DEPARTEMENTS_7_SFS =
      readEuropeFeatureCollectionAsList(7);
  private static final List<SimpleFeature> FRANCE_DEPARTEMENTS_8_SFS =
      readEuropeFeatureCollectionAsList(8);
  private static final List<SimpleFeature> FRANCE_DEPARTEMENTS_9_SFS =
      readEuropeFeatureCollectionAsList(9);
  private static final List<SimpleFeature> FRANCE_DEPARTEMENTS_10_SFS =
      readEuropeFeatureCollectionAsList(10);
  private static final List<SimpleFeature> QUEBEC_1_SFS = readEuropeFeatureCollectionAsList(11);
  private static final List<SimpleFeature> LUXEMEBOURG = readEuropeFeatureCollectionAsList(12);
  private static final List<SimpleFeature> SUISSE = readEuropeFeatureCollectionAsList(13);

  @SneakyThrows
  private static List<SimpleFeature> readEuropeFeatureCollectionAsList(
      @Range(from = 0, to = 12) int number) {
    return getSimpleFeatures("departements_%s.json".formatted(number));
  }

  public static List<SimpleFeature> getFranceAndQuebecDepartementsSimpleFeaturesMatchingPredicate(
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
            FRANCE_DEPARTEMENTS_10_SFS,
            QUEBEC_1_SFS,
            LUXEMEBOURG,
            SUISSE);
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
