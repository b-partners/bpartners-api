package app.bpartners.api.endpoint.rest.mapper;

import static app.bpartners.api.endpoint.rest.model.OpenStreetMapLayer.TOUS_FR;

import app.bpartners.api.endpoint.rest.model.AreaPictureDetails;
import app.bpartners.api.endpoint.rest.model.CrupdateAreaPictureDetails;
import app.bpartners.api.endpoint.rest.model.OpenStreetMapLayer;
import app.bpartners.api.endpoint.rest.validator.CrupdateAreaPictureDetailsValidator;
import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.service.WMS.AreaPictureMapLayerService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AreaPictureRestMapper {
  private final CrupdateAreaPictureDetailsValidator validator;
  private final AreaPictureMapLayerRestMapper layerRestMapper;
  @Deprecated private final AreaPictureMapLayerService areaPictureMapLayer;

  public AreaPictureDetails toRest(AreaPicture domain) {
    return new AreaPictureDetails()
        .id(domain.getId())
        .fileId(domain.getIdFileInfo())
        .filename(domain.getFilename())
        .address(domain.getAddress())
        .zoomLevel(domain.getZoomLevel())
        .createdAt(domain.getCreatedAt())
        .updatedAt(domain.getUpdatedAt())
        .xTile(domain.getTile().getX())
        .yTile(domain.getTile().getY())
        .prospectId(domain.getIdProspect())
        .layer(TOUS_FR)
        .availableLayers(List.of(TOUS_FR))
        .actualLayer(layerRestMapper.toRest(domain.getCurrentLayer()))
        .otherLayers(domain.getLayers().stream().map(layerRestMapper::toRest).toList());
  }

  public AreaPicture toDomain(CrupdateAreaPictureDetails rest, String id, String userId) {
    AreaPictureMapLayer mapLayer;
    validator.accept(rest);
    OpenStreetMapLayer restOsmLayer = rest.getLayer();
    if (TOUS_FR.equals(restOsmLayer)) {
      mapLayer = areaPictureMapLayer.getDefaultLayer();
    } else {
      mapLayer = rest.getLayerId() == null ? null : layerRestMapper.toDomain(rest.getLayerId());
    }
    return AreaPicture.builder()
        .id(id)
        .address(rest.getAddress())
        .idFileInfo(rest.getFileId())
        .zoomLevel(rest.getZoomLevel())
        .idProspect(rest.getProspectId())
        .currentLayer(mapLayer)
        .idUser(userId)
        .createdAt(rest.getCreatedAt())
        .updatedAt(rest.getUpdatedAt())
        .build();
  }
}
