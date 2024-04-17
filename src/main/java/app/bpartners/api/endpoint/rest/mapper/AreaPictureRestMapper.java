package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.AreaPictureDetails;
import app.bpartners.api.endpoint.rest.model.CrupdateAreaPictureDetails;
import app.bpartners.api.endpoint.rest.model.OpenStreetMapLayer;
import app.bpartners.api.endpoint.rest.validator.CrupdateAreaPictureDetailsValidator;
import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.service.WMS.MapLayer;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AreaPictureRestMapper {
  private final CrupdateAreaPictureDetailsValidator validator;

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
        .layer(OpenStreetMapLayer.fromValue(domain.getCurrentLayer().getValue()))
        .availableLayers(
            domain.getLayers().stream()
                .map(MapLayer::getValue)
                .map(OpenStreetMapLayer::fromValue)
                .collect(Collectors.toUnmodifiableList()));
  }

  public AreaPicture toDomain(CrupdateAreaPictureDetails rest, String id, String userId) {
    validator.accept(rest);
    var layer = rest.getLayer() == null ? null : MapLayer.from(rest.getLayer());
    return AreaPicture.builder()
        .id(id)
        .address(rest.getAddress())
        .idFileInfo(rest.getFileId())
        .zoomLevel(rest.getZoomLevel())
        .idProspect(rest.getProspectId())
        .currentLayer(layer)
        .idUser(userId)
        .createdAt(rest.getCreatedAt())
        .updatedAt(rest.getUpdatedAt())
        .build();
  }
}
