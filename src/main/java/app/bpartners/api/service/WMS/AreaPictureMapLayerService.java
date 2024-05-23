package app.bpartners.api.service.WMS;

import static app.bpartners.api.service.WMS.GeojsonFeatureCollection.getFranceDepartementsSimpleFeaturesMatchingPredicate;

import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.AreaPictureMapLayerRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class AreaPictureMapLayerService {
  public static final int WGS_84_SRID = 4326;
  private final AreaPictureMapLayerRepository repository;

  public List<AreaPictureMapLayer> getAvailableLayersFrom(Tile tile) {
    var geometryFactory = new GeometryFactory(new PrecisionModel(), WGS_84_SRID);
    var areaPictureCoordinatesAsPoint =
        geometryFactory.createPoint(new Coordinate(tile.getLongitude(), tile.getLatitude()));
    List<SimpleFeature> features =
        getFranceDepartementsSimpleFeaturesMatchingPredicate(
            feature -> {
              var geometry = (Geometry) feature.getDefaultGeometry();
              return geometry.contains(areaPictureCoordinatesAsPoint);
            });
    if (features.isEmpty()) {
      return List.of(getDefaultLayer());
    }
    List<String> matchingFeaturesName =
        features.stream().map(f -> (String) f.getAttribute("nom")).toList();
    var layers = getAllByDepartementNameInIgnoreCaseOrderByYearAndAddDefault(matchingFeaturesName);
    if (layers.isEmpty()) {
      log.info("no layer found for {}", tile);
      return List.of(getDefaultLayer());
    }
    return layers;
  }

  private List<AreaPictureMapLayer> getAllByDepartementNameInIgnoreCaseOrderByYearAndAddDefault(
      List<String> matchingFeaturesName) {
    var result =
        new ArrayList<>(
            repository.findAllByDepartementNameInIgnoreCaseOrderByYear(matchingFeaturesName));
    result.add(getDefaultLayer());
    return result;
  }

  public AreaPictureMapLayer getLatestMostPreciseOrDefault(List<AreaPictureMapLayer> layers) {
    return layers.stream()
        .max(AreaPictureMapLayer::compareTo)
        .orElse(getDefaultLayer());
  }

  public AreaPictureMapLayer getDefaultLayer() {
    return getById("2cb589c1-45b0-4cb8-b84e-f1ed40e97bd8");
  }

  public AreaPictureMapLayer getById(String id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("AreaPictureMapLayer.Id = " + id + " not found"));
  }
}
