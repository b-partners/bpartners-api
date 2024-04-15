package app.bpartners.api.endpoint.rest.mapper;

import static app.bpartners.api.endpoint.rest.model.OpenStreetMapLayer.TOUS_FR;

import app.bpartners.api.endpoint.rest.model.AreaPictureDetails;
import app.bpartners.api.endpoint.rest.model.CrupdateAreaPictureDetails;
import app.bpartners.api.endpoint.rest.model.OpenStreetMapLayer;
import app.bpartners.api.endpoint.rest.validator.CrupdateAreaPictureDetailsValidator;
import app.bpartners.api.model.AreaPicture;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AreaPictureRestMapper {
  public static final OpenStreetMapLayer DEFAULT_FRANCE_LAYER = TOUS_FR;
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
        .layer(domain.getLayer());
  }

  public AreaPicture toDomain(CrupdateAreaPictureDetails rest, String id, String userId) {
    validator.accept(rest);
    return AreaPicture.builder()
        .id(id)
        .address(rest.getAddress())
        .idFileInfo(rest.getFileId())
        .filename(rest.getFilename())
        .zoomLevel(rest.getZoomLevel())
        .idProspect(rest.getProspectId())
        .layer(DEFAULT_FRANCE_LAYER)
        .idUser(userId)
        .createdAt(rest.getCreatedAt())
        .updatedAt(rest.getUpdatedAt())
        .build();
  }
}
