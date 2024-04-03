package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.AreaPictureDetails;
import app.bpartners.api.model.AreaPicture;
import org.springframework.stereotype.Component;

@Component
public class AreaPictureRestMapper {
  public AreaPictureDetails toRest(AreaPicture domain) {
    return new AreaPictureDetails()
        .id(domain.getId())
        .fileId(domain.getIdFileInfo())
        .filename(domain.getFilename())
        .address(domain.getAddress())
        .zoomLevel(domain.getZoomLevel())
        .createdAt(domain.getCreatedAt())
        .updatedAt(domain.getUpdatedAt())
        .layer(domain.getLayer());
  }
}
