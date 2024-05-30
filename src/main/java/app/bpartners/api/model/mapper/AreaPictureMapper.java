package app.bpartners.api.model.mapper;

import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.model.validator.AreaPictureValidator;
import app.bpartners.api.repository.jpa.model.HAreaPicture;
import app.bpartners.api.service.WMS.AreaPictureMapLayerService;
import app.bpartners.api.service.WMS.Tile;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AreaPictureMapper {
  private final AreaPictureValidator validator;
  private final AreaPictureMapLayerService areaPictureMapLayerService;

  public AreaPicture toDomain(HAreaPicture entity) {
    AreaPictureMapLayer layer = areaPictureMapLayerService.getById(entity.getIdLayer());
    var domain =
        AreaPicture.builder()
            .id(entity.getId())
            .address(entity.getAddress())
            .latitude(entity.getLatitude())
            .longitude(entity.getLongitude())
            .zoomLevel(entity.getZoomLevel())
            .currentLayer(layer)
            .idUser(entity.getIdUser())
            .idFileInfo(entity.getIdFileInfo())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .idProspect(entity.getIdProspect())
            .isExtended(entity.isExtended())
            .build();
    Tile tile = Tile.from(domain);
    domain.setTile(tile);
    domain.setLayers(areaPictureMapLayerService.getAvailableLayersFrom(tile));
    return domain;
  }

  public HAreaPicture toEntity(AreaPicture domain) {
    validator.accept(domain);
    return HAreaPicture.builder()
        .id(domain.getId())
        .address(domain.getAddress())
        .filename(domain.getFilename())
        .latitude(domain.getLatitude())
        .longitude(domain.getLongitude())
        .zoomLevel(domain.getZoomLevel())
        .idLayer(domain.getCurrentLayer().getId())
        .idUser(domain.getIdUser())
        .idFileInfo(domain.getIdFileInfo())
        .createdAt(domain.getCreatedAt())
        .updatedAt(domain.getUpdatedAt())
        .idProspect(domain.getIdProspect())
        .isExtended(domain.isExtended())
        .build();
  }
}
