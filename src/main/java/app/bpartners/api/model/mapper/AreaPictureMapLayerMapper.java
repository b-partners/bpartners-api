package app.bpartners.api.model.mapper;

import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.repository.jpa.model.HAreaPictureMapLayer;
import org.springframework.stereotype.Component;

@Component
public class AreaPictureMapLayerMapper {
  public AreaPictureMapLayer toDomain(HAreaPictureMapLayer areaPictureMapLayer) {
    return AreaPictureMapLayer.builder()
        .id(areaPictureMapLayer.getId())
        .name(areaPictureMapLayer.getName())
        .year(areaPictureMapLayer.getYear())
        .source(areaPictureMapLayer.getSource())
        .departementName(areaPictureMapLayer.getDepartementName())
        .maximumZoomLevel(areaPictureMapLayer.getMaximumZoomLevel())
        .precisionLevelInCm(areaPictureMapLayer.getPrecisionLevelInCm())
        .build();
  }

  public AreaPictureMapLayer toDomain(
      app.bpartners.api.endpoint.rest.model.AreaPictureMapLayer rest) {
    return AreaPictureMapLayer.builder()
        .id(rest.getId())
        .name(rest.getName())
        .year(rest.getYear())
        .source(rest.getSource())
        .departementName(rest.getDepartementName())
        .maximumZoomLevel(rest.getMaximumZoomLevel())
        .precisionLevelInCm(rest.getPrecisionLevelInCm())
        .build();
  }
}
