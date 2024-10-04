package app.bpartners.api.endpoint.rest.mapper;

import static app.bpartners.api.endpoint.rest.model.OpenStreetMapLayer.TOUS_FR;

import app.bpartners.api.endpoint.rest.model.AreaPictureDetails;
import app.bpartners.api.endpoint.rest.model.CrupdateAreaPictureDetails;
import app.bpartners.api.endpoint.rest.model.Tile;
import app.bpartners.api.endpoint.rest.model.Zoom;
import app.bpartners.api.endpoint.rest.model.ZoomLevel;
import app.bpartners.api.endpoint.rest.validator.CrupdateAreaPictureDetailsValidator;
import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.service.WMS.AreaPictureMapLayerService;
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
  @Deprecated private final AreaPictureMapLayerService areaPictureMapLayerService;

  private static Tile toRestTile(app.bpartners.api.service.WMS.Tile domain, Zoom zoom) {
    return new Tile().x(domain.getX()).y(domain.getY()).zoom(zoom);
  }

  public AreaPictureDetails toRest(AreaPicture domain) {
    var arcgisZoom = domain.getArcgisZoom();
    Zoom zoom = new Zoom().level(domain.getZoomLevel()).number(arcgisZoom.getZoomLevel());
    var tile = toRestTile(domain.getCurrentTile(), zoom);
    Tile referenceTile = toRestTile(domain.getReferenceTile(), zoom);
    return new AreaPictureDetails()
        .id(domain.getId())
        .fileId(domain.getIdFileInfo())
        .filename(domain.getFilename())
        .address(domain.getAddress())
        .zoomLevel(domain.getZoomLevel())
        .createdAt(domain.getCreatedAt())
        .updatedAt(domain.getUpdatedAt())
        .xTile(tile.getX())
        .yTile(tile.getY())
        .prospectId(domain.getIdProspect())
        .zoom(zoom)
        .layer(TOUS_FR)
        .availableLayers(List.of(TOUS_FR))
        .actualLayer(layerRestMapper.toRest(domain.getCurrentLayer()))
        .otherLayers(domain.getLayers().stream().map(layerRestMapper::toRest).toList())
        .currentGeoPosition(domain.getCurrentGeoPosition())
        .geoPositions(domain.getGeoPositions())
        .currentTile(tile)
        .referenceTile(referenceTile)
        .isExtended(domain.isExtended());
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
    return AreaPicture.builder()
        .id(id)
        .address(rest.getAddress())
        .idFileInfo(rest.getFileId())
        .zoomLevel(zoomLevel)
        .idProspect(rest.getProspectId())
        .currentLayer(mapLayer)
        .idUser(userId)
        .createdAt(rest.getCreatedAt())
        .updatedAt(rest.getUpdatedAt())
        .isExtended(isExtended != null && isExtended)
        .rightShift(rest.getRightShift() == null ? false : rest.getRightShift())
        .leftShift(rest.getLeftShift() == null ? false : rest.getLeftShift())
        .shiftNb(rest.getShiftNb() == null ? null : rest.getShiftNb())
        .build();
  }
}
