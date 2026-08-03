package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.mapper.AreaPictureRestMapper;
import app.bpartners.api.endpoint.rest.model.AreaPictureDetails;
import app.bpartners.api.endpoint.rest.model.CrupdateAreaPictureDetails;
import app.bpartners.api.endpoint.rest.model.GeoPosition;
import app.bpartners.api.endpoint.rest.model.ShiftDirection;
import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.model.validator.AreaPictureValidator;
import app.bpartners.api.repository.jpa.model.HAreaPicture;
import app.bpartners.api.service.wms.AreaPictureMapLayerService;
import app.bpartners.api.service.wms.Tile;
import app.bpartners.api.service.wms.imageSource.TileExtenderRequestBody;
import java.time.Instant;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class AreaPictureMapper {
  private final AreaPictureValidator validator;
  private final AreaPictureMapLayerService areaPictureMapLayerService;
  private final AreaPictureMapLayerMapper areaPictureMapLayerMapper;
  private final AreaPictureRestMapper areaPictureRestMapper;

  @SneakyThrows
  public AreaPicture toDomain(HAreaPicture entity) {
    AreaPictureMapLayer layer = areaPictureMapLayerService.getById(entity.getIdLayer());
    var domain =
        AreaPicture.builder()
            .id(entity.getId())
            .address(entity.getAddress())
            .currentGeoPosition(entity.getCurrentGeoPosition())
            .zoomLevel(entity.getZoomLevel())
            .currentLayer(layer)
            .idUser(entity.getIdUser())
            .idFileInfo(entity.getIdFileInfo())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .idProspect(entity.getIdProspect())
            .isExtended(entity.isExtended())
            .geoPositions(entity.getGeoPositions())
            .shiftNb(entity.getShiftNb())
            .shiftDirection(entity.getShiftDirection())
            .build();
    Tile tile = Tile.from(domain);
    domain.setCurrentTile(tile);
    domain.setLayers(
        areaPictureMapLayerService.getAvailableLayersFrom(entity.getCurrentGeoPosition()));
    return domain;
  }

  public HAreaPicture toEntity(AreaPicture domain) {
    validator.accept(domain);
    GeoPosition currentGeoPosition = Objects.requireNonNull(domain.getCurrentGeoPosition());
    return HAreaPicture.builder()
        .id(domain.getId())
        .address(domain.getAddress())
        .filename(domain.getFilename())
        .latitude(currentGeoPosition.getLatitude())
        .longitude(currentGeoPosition.getLongitude())
        .score(currentGeoPosition.getScore())
        .zoomLevel(domain.getZoomLevel())
        .idLayer(domain.getCurrentLayer().getId())
        .idUser(domain.getIdUser())
        .idFileInfo(domain.getIdFileInfo())
        .createdAt(domain.getCreatedAt())
        .updatedAt(domain.getUpdatedAt())
        .idProspect(domain.getIdProspect())
        .isExtended(domain.isExtended())
        .geoPositions(domain.getGeoPositions())
        .shiftNb(domain.getShiftNb())
        .shiftDirection(domain.getShiftDirection())
        .build();
  }

  public AreaPicture toDomain(
      AreaPictureDetails areaPictureDetails,
      String areaPictureId,
      String idUser,
      String idProspect) {
    return AreaPicture.builder()
        .id(areaPictureId)
        .address(areaPictureDetails.getAddress())
        .zoomLevel(areaPictureDetails.getZoomLevel())
        .idUser(idUser)
        .idFileInfo(areaPictureDetails.getFileId())
        .createdAt(areaPictureDetails.getCreatedAt())
        .updatedAt(areaPictureDetails.getUpdatedAt())
        .idProspect(idProspect)
        .isExtended(Boolean.TRUE.equals(areaPictureDetails.getIsExtended()))
        .geoPositions(areaPictureDetails.getGeoPositions())
        .shiftNb(areaPictureDetails.getShiftNb())
        .currentLayer(
            areaPictureMapLayerMapper.toDomain(
                Objects.requireNonNull(areaPictureDetails.getActualLayer())))
        .currentTile(
            Tile.builder()
                .arcgisZoom(null)
                .x(areaPictureDetails.getCurrentTile().getX())
                .y(areaPictureDetails.getCurrentTile().getY())
                .build())
        .currentGeoPosition(areaPictureDetails.getCurrentGeoPosition())
        .shiftDirection(
            areaPictureDetails.getShiftDirection() == null
                ? null
                : areaPictureRestMapper.toDomain(areaPictureDetails.getShiftDirection()))
        .build();
  }

  public CrupdateAreaPictureDetails toCrupdatedAreaPictureDetails(AreaPicture areaPicture) {
    return new CrupdateAreaPictureDetails()
        .shiftNb(areaPicture.getShiftNb())
        .address(areaPicture.getAddress())
        .fileId(areaPicture.getIdFileInfo())
        .zoomLevel(areaPicture.getZoomLevel())
        .isExtended(areaPicture.isExtended())
        .prospectId(areaPicture.getIdProspect())
        .shiftDirection(
            areaPicture.getShiftDirection() == null
                ? null
                : toRest(areaPicture.getShiftDirection()))
        .createdAt(Instant.now())
        .layerId(
            areaPicture.getCurrentLayer() != null ? areaPicture.getCurrentLayer().getId() : null)
        .isOpaque(areaPicture.isOpaque());
  }

  public ShiftDirection toRest(TileExtenderRequestBody.ShiftDirection shiftDirection) {
    return shiftDirection.equals(TileExtenderRequestBody.ShiftDirection.RIGHT_LEFT_SIDE)
        ? ShiftDirection.RIGHT_LEFT_SIDE
        : ShiftDirection.UP_DOWN_SIDE;
  }
}
