package app.bpartners.api.model.mapper;

import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.repository.jpa.model.HAreaPicture;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AreaPictureMapper {
  public AreaPicture toDomain(HAreaPicture entity) {
    return AreaPicture.builder()
        .id(entity.getId())
        .address(entity.getAddress())
        .filename(entity.getFilename())
        .latitude(entity.getLatitude())
        .longitude(entity.getLongitude())
        .zoomLevel(entity.getZoomLevel())
        .layer(entity.getLayer())
        .idUser(entity.getIdUser())
        .idFileInfo(entity.getIdFileInfo())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }
}
