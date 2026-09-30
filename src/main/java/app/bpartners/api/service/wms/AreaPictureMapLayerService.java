package app.bpartners.api.service.wms;

import app.bpartners.api.endpoint.rest.model.GeoPosition;
import app.bpartners.api.endpoint.rest.model.MapLayerActual;
import app.bpartners.api.endpoint.rest.model.MapLayersReachability;
import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.model.mapper.AreaPictureMapLayerMapper;
import app.bpartners.api.service.geodata.ImageryService;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class AreaPictureMapLayerService {
  public static final int WGS_84_SRID = 4326;
  private final ImageryService imageryService;
  private final AreaPictureMapLayerMapper areaPictureMapLayerMapper;

  public List<AreaPictureMapLayer> getAvailableLayersFrom(GeoPosition geoPosition) {
    return getAvailableLayersFrom(geoPosition.getLongitude(), geoPosition.getLatitude());
  }

  public List<AreaPictureMapLayer> getAvailableLayersFrom(Double longitude, Double latitude) {
    return imageryService.getMapLayersFrom(longitude, latitude).stream()
        .map(areaPictureMapLayerMapper::toDomain)
        .toList();
  }

  public MapLayersReachability getMapLayers(
      Double latitude, Double longitude, boolean onlyReachable) {
    return imageryService.getMapLayers(latitude, longitude, onlyReachable);
  }

  public MapLayerActual getActualMapLayer(Double latitude, Double longitude) {
    return imageryService.getActualMapLayer(latitude, longitude);
  }

  public AreaPictureMapLayer getById(String id) {
    return areaPictureMapLayerMapper.toDomain(imageryService.getById(id));
  }
}
