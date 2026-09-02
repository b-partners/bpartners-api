package app.bpartners.api.endpoint.rest.mapper;

import static app.bpartners.api.endpoint.rest.model.OpenStreetMapLayer.TOUS_FR;

import app.bpartners.api.endpoint.rest.model.AreaPictureDetails;
import app.bpartners.api.endpoint.rest.model.CrupdateAreaPictureDetails;
import app.bpartners.api.endpoint.rest.model.ShiftDirection;
import app.bpartners.api.endpoint.rest.model.Tile;
import app.bpartners.api.endpoint.rest.model.Zoom;
import app.bpartners.api.endpoint.rest.model.ZoomLevel;
import app.bpartners.api.endpoint.rest.validator.CrupdateAreaPictureDetailsValidator;
import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.service.areapicture.MetaDataComponent;
import app.bpartners.api.service.wms.AreaPictureMapLayerService;
import app.bpartners.api.service.wms.imageSource.TileExtenderRequestBody;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class AreaPictureRestMapper {
  private final CrupdateAreaPictureDetailsValidator validator;
  private final AreaPictureMapLayerRestMapper layerRestMapper;
  private final MetaDataComponent metaDataComponent;
  @Deprecated private final AreaPictureMapLayerService areaPictureMapLayerService;

  private static Tile toRestTile(app.bpartners.api.service.wms.Tile domain, Zoom zoom) {
    return new Tile().x(domain.getX()).y(domain.getY()).zoom(zoom);
  }

  public AreaPictureDetails toRest(AreaPicture domain) {
    var arcgisZoom = domain.getArcgisZoom();
    Zoom zoom = new Zoom().level(domain.getZoomLevel()).number(arcgisZoom.getZoomLevel());
    boolean hasImage = domain.getCurrentTile() != null;
    var tile = hasImage ? toRestTile(domain.getCurrentTile(), zoom) : null;
    Tile referenceTile = hasImage ? toRestTile(domain.getReferenceTile(), zoom) : null;
    int xOffset = metaDataComponent.getXOffset();
    int yOffset = metaDataComponent.getYOffset();
    log.info("Layers={}", domain.getLayers());

    return new AreaPictureDetails()
        .id(domain.getId())
        .fileId(domain.getIdFileInfo())
        .filename(domain.getFilename())
        .address(domain.getAddress())
        .zoomLevel(domain.getZoomLevel())
        .createdAt(domain.getCreatedAt())
        .updatedAt(domain.getUpdatedAt())
        .xTile(tile == null ? null : tile.getX())
        .yTile(tile == null ? null : tile.getY())
        .prospectId(domain.getIdProspect())
        .zoom(zoom)
        .layer(TOUS_FR)
        .availableLayers(List.of(TOUS_FR))
        .actualLayer(
            domain.getCurrentLayer() == null
                ? null
                : layerRestMapper.toRest(domain.getCurrentLayer()))
        .otherLayers(
            domain.getLayers() == null
                ? List.of()
                : domain.getLayers().stream().map(layerRestMapper::toRest).toList())
        .currentGeoPosition(domain.getCurrentGeoPosition())
        .geoPositions(domain.getGeoPositions())
        .currentTile(tile)
        .referenceTile(referenceTile)
        .shiftNb(domain.getShiftNb())
        .yOffset(yOffset)
        .xOffset(xOffset)
        .isExtended(domain.isExtended())
        .shiftDirection(toRest(domain.getShiftDirection()))
        .isOpaque(domain.isOpaque());
  }

  public AreaPicture toDomain(CrupdateAreaPictureDetails rest, String id, String userId) {
    AreaPictureMapLayer mapLayer;
    validator.accept(rest);
    mapLayer = rest.getLayerId() == null ? null : layerRestMapper.toDomain(rest.getLayerId());
    ZoomLevel zoomLevel;
    Zoom zoom = rest.getZoom();
    if (zoom == null) {
      log.info("DEPRECATED, USE ZOOM");
      zoomLevel = rest.getZoomLevel();
    } else {
      zoomLevel = zoom.getLevel();
    }
    Boolean isExtended = rest.getIsExtended();
    ShiftDirection restShiftDirection = rest.getShiftDirection();
    Boolean downloadImage = rest.getDownloadImage();
    return AreaPicture.builder()
        .id(id)
        .address(rest.getAddress())
        .idFileInfo(rest.getFileId())
        .zoomLevel(zoomLevel)
        .idProspect(rest.getProspectId())
        .currentLayer(mapLayer)
        .initialLayer(mapLayer)
        .idUser(userId)
        .createdAt(rest.getCreatedAt())
        .updatedAt(rest.getUpdatedAt())
        .isExtended(isExtended != null && isExtended)
        .shiftNb(rest.getShiftNb() == null ? null : rest.getShiftNb())
        .isOpaque(Boolean.TRUE.equals(rest.getIsOpaque()))
        .shiftDirection(restShiftDirection != null ? toDomain(restShiftDirection) : null)
        .downloadImage(downloadImage == null || downloadImage)
        .build();
  }

  public ShiftDirection toRest(TileExtenderRequestBody.ShiftDirection domain) {
    if (domain == null) return null;

    return domain == TileExtenderRequestBody.ShiftDirection.RIGHT_LEFT_SIDE
        ? ShiftDirection.RIGHT_LEFT_SIDE
        : ShiftDirection.UP_DOWN_SIDE;
  }

  public TileExtenderRequestBody.ShiftDirection toDomain(ShiftDirection rest) {
    return "RIGHT_LEFT_SIDE".equals(rest.getValue())
        ? TileExtenderRequestBody.ShiftDirection.RIGHT_LEFT_SIDE
        : TileExtenderRequestBody.ShiftDirection.UP_DOWN_SIDE;
  }
}
