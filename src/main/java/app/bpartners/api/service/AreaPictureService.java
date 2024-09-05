package app.bpartners.api.service;

import static app.bpartners.api.endpoint.rest.model.FileType.AREA_PICTURE;

import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.AreaPictureMapper;
import app.bpartners.api.repository.AccountHolderRepository;
import app.bpartners.api.repository.AccountRepository;
import app.bpartners.api.repository.jpa.AreaPictureJpaRepository;
import app.bpartners.api.service.WMS.AreaPictureMapLayerService;
import app.bpartners.api.service.WMS.Tile;
import app.bpartners.api.service.WMS.TileCreator;
import app.bpartners.api.service.WMS.imageSource.WmsImageSource;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class AreaPictureService {
  private final AreaPictureJpaRepository jpaRepository;
  private final AreaPictureMapper mapper;
  private final FileService fileService;
  private final WmsImageSource wmsImageSource;
  private final TileCreator tileCreator;
  private final AreaPictureMapLayerService mapLayerService;
  private final AccountRepository accountRepository;
  private final AccountHolderRepository accountHolderRepository;

  public List<AreaPicture> findAllBy(String userId, String address, String filename) {
    return jpaRepository
        .findAllByIdUserAndAddressContainingIgnoreCaseAndFilenameContainingIgnoreCase(
            userId, address, filename)
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  public AreaPicture findBy(String userId, String id) {
    var domain =
        mapper.toDomain(
            jpaRepository
                .findByIdUserAndId(userId, id)
                .orElseThrow(
                    () ->
                        new NotFoundException(
                            "HAreaPicture with UserId = "
                                + userId
                                + " and Id = "
                                + id
                                + " was not found.")));
    return domain;
  }

  @Transactional
  public AreaPicture downloadFromExternalSourceAndSave(AreaPicture areaPicture)
      throws RuntimeException {
    var refreshed = refreshAreaPictureTileAndLayers(areaPicture);
    var downloadedFile = wmsImageSource.downloadImage(areaPicture);
    fileService.upload(
        AREA_PICTURE, refreshed.getIdFileInfo(), refreshed.getIdUser(), downloadedFile);
    return save(refreshed);
  }

  private AreaPicture refreshAreaPictureTileAndLayers(AreaPicture areaPicture) {
    refreshAreaPictureTile(areaPicture);
    refreshAreaPictureMapLayers(areaPicture);
    return areaPicture;
  }

  private void refreshAreaPictureMapLayers(AreaPicture areaPicture) {
    var guessedMaps = mapLayerService.getAvailableLayersFrom(areaPicture.getCurrentTile());
    if (areaPicture.getCurrentLayer() == null) {
      var latest = mapLayerService.getLatestMostPreciseOrDefault(guessedMaps);
      areaPicture.setCurrentLayer(latest);
    }
    areaPicture.setLayers(guessedMaps);
  }

  private void refreshAreaPictureTile(AreaPicture areaPicture) {
    Tile tile = tileCreator.apply(areaPicture);
    areaPicture.setCurrentTile(tile);
  }

  @Transactional
  public AreaPicture save(AreaPicture areaPicture) {
    return mapper.toDomain(jpaRepository.save(mapper.toEntity(areaPicture)));
  }
}
