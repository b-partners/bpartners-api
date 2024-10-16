package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.AreaPictureMapLayer;
import app.bpartners.api.endpoint.rest.model.Zoom;
import app.bpartners.api.service.WMS.AreaPictureMapLayerService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AreaPictureMapLayerRestMapper {
  private final AreaPictureMapLayerService mapLayerService;

  public AreaPictureMapLayer toRest(app.bpartners.api.model.AreaPictureMapLayer domain) {
    var maxArcgisZoom = domain.getMaxZoomLevelAsArcgisZoom();
    Zoom maxZoom =
        new Zoom().level(domain.getMaximumZoomLevel()).number(maxArcgisZoom.getZoomLevel());
    return new AreaPictureMapLayer()
        .id(domain.getId())
        .name(domain.getName())
        .year(domain.getYear())
        .departementName(domain.getDepartementName())
        .maximumZoomLevel(domain.getMaximumZoomLevel())
        .maximumZoom(maxZoom)
        .precisionLevelInCm(domain.getPrecisionLevelInCm())
        .source(domain.getSource());
  }

  public app.bpartners.api.model.AreaPictureMapLayer toDomain(String restId) {
    return mapLayerService.getById(restId);
  }
}
