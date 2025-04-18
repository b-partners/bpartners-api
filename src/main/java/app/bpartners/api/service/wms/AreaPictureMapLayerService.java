package app.bpartners.api.service.wms;

import static app.bpartners.api.service.wms.GeojsonFeatureCollection.getFranceDepartementsSimpleFeaturesMatchingPredicate;

import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.AreaPictureMapLayerRepository;
import java.util.ArrayList;
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
  public static final String DEFAULT_IGN_LAYER_UUID = "1cccfc17-cbef-4320-bdfa-0d1920b91f11";
  public static final String DEFAULT_PCRS_LAYER_UUID = "726f5b3b-d23b-40c3-b38e-68a43d7ae155";
  public static final String DEFAULT_AERIAL_PHOTOGRAPHY_LAYER_UUID =
      "2f343dba-dd5f-4895-9006-49472f576c02";
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
      return List.of(getPCRSLayer(), getAerialPhotography(), getDefaultIGNLayer());
    }
    List<String> matchingFeaturesName =
        features.stream().map(f -> (String) f.getAttribute("nom")).toList();
    log.info("Features name: {}", matchingFeaturesName);
    var layers = getAllByDepartementNameInIgnoreCaseOrderByYearAndAddDefault(matchingFeaturesName);
    if (layers.isEmpty()) {
      log.info("no layer found for {}", tile);
      return List.of(getDefaultIGNLayer(), getPCRSLayer(), getAerialPhotography());
    }
    return layers;
  }

  private List<AreaPictureMapLayer> getAllByDepartementNameInIgnoreCaseOrderByYearAndAddDefault(
      List<String> matchingFeaturesName) {
    var result =
        new ArrayList<>(
            repository.findAllByDepartementNameInIgnoreCaseOrderByYear(matchingFeaturesName));
    result.addAll(List.of(getPCRSLayer(), getAerialPhotography(), getDefaultIGNLayer()));
    return result;
  }

  public AreaPictureMapLayer getLatestMostPreciseOrDefault(List<AreaPictureMapLayer> layers) {
    return layers.stream().max(AreaPictureMapLayer::compareTo).orElse(getDefaultIGNLayer());
  }

  public AreaPictureMapLayer getDefaultIGNLayer() {
    return getById(DEFAULT_IGN_LAYER_UUID);
  }

  public AreaPictureMapLayer getPCRSLayer() {
    return getById(DEFAULT_PCRS_LAYER_UUID);
  }

  public AreaPictureMapLayer getAerialPhotography() {
    return getById(DEFAULT_AERIAL_PHOTOGRAPHY_LAYER_UUID);
  }

  public AreaPictureMapLayer getById(String id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("AreaPictureMapLayer.Id = " + id + " not found"));
  }
}
