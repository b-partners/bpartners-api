package app.bpartners.api.endpoint.rest.mapper;

import static app.bpartners.api.endpoint.rest.model.AreaPictureImageSource.AIRBUS;

import app.bpartners.api.endpoint.rest.model.AreaPictureMapLayer;
import app.bpartners.api.endpoint.rest.model.Zoom;
import app.bpartners.api.service.areapicture.MetaDataComponent;
import app.bpartners.api.service.wms.AreaPictureMapLayerService;
import java.io.IOException;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AreaPictureMapLayerRestMapper {
  private final AreaPictureMapLayerService mapLayerService;
  private final MetaDataComponent metaDataComponent;

  public AreaPictureMapLayer toRest(app.bpartners.api.model.AreaPictureMapLayer domain) {
    var maxArcgisZoom = domain.getMaxZoomLevelAsArcgisZoom();
    Zoom maxZoom =
        new Zoom().level(domain.getMaximumZoomLevel()).number(maxArcgisZoom.getZoomLevel());
    int precision = domain.getPrecisionLevelInCm();
    int year = domain.getYear();
    LocalDate date = LocalDate.of(year, 1, 1);
    if (domain.getSource().equals(AIRBUS)) {
      year = metaDataComponent.getAirbusYear();
      date = metaDataComponent.getLastUpdatedAt();
    }
    return new AreaPictureMapLayer()
        .id(domain.getId())
        .name(domain.getName())
        .year(year)
        .lastUpdatedAt(date)
        .departementName(domain.getDepartementName())
        .maximumZoomLevel(domain.getMaximumZoomLevel())
        .maximumZoom(maxZoom)
        .precisionLevelInCm(precision)
        .source(domain.getSource());
  }

  public app.bpartners.api.model.AreaPictureMapLayer toDomain(String restId)
      throws IOException, InterruptedException {
    return mapLayerService.getById(restId);
  }
}
