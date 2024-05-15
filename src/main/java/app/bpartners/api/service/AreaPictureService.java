package app.bpartners.api.service;

import static app.bpartners.api.endpoint.rest.model.FileType.AREA_PICTURE;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.api.file.FileDownloader;
import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.AreaPictureMapper;
import app.bpartners.api.repository.jpa.AreaPictureJpaRepository;
import app.bpartners.api.service.WMS.AreaPictureMapLayerService;
import app.bpartners.api.service.WMS.Tile;
import app.bpartners.api.service.WMS.TileCreator;
import app.bpartners.api.service.WMS.imageSource.WmsImageSource;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
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
  private final FileDownloader fileDownloader = new FileDownloader(HttpClient.newBuilder().build());
  private final WmsImageSource wmsImageSource;
  private final TileCreator tileCreator;
  private final AreaPictureMapLayerService mapLayerService;

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
    domain.setLayers(mapLayerService.getAvailableLayersFrom(domain.getTile()));
    return domain;
  }

  @Transactional
  public AreaPicture downloadFromExternalSourceAndSave(AreaPicture areaPicture)
      throws RuntimeException {
    var refreshed = refreshAreaPictureTileAndLayers(areaPicture);
    var uri = wmsImageSource.apply(refreshed.getTile(), refreshed.getCurrentLayer());
    var downloadedFile = fileDownloader.apply(refreshed.getFilename(), uri);
    try {
      var downloadedFileAsBytes = Files.readAllBytes(downloadedFile.toPath());
      fileService.upload(
          refreshed.getIdFileInfo(), AREA_PICTURE, refreshed.getIdUser(), downloadedFileAsBytes);
      var saved = save(refreshed);
      saved.setLayers(refreshed.getLayers());
      return saved;
    } catch (IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  private AreaPicture refreshAreaPictureTileAndLayers(AreaPicture areaPicture) {
    refreshAreaPictureTile(areaPicture);
    refreshAreaPictureMapLayers(areaPicture);
    return areaPicture;
  }

  private void refreshAreaPictureMapLayers(AreaPicture areaPicture) {
    var guessedMaps = mapLayerService.getAvailableLayersFrom(areaPicture.getTile());
    var latest = mapLayerService.getLatestOrDefault(guessedMaps);
    areaPicture.setCurrentLayer(latest);
    areaPicture.setLayers(guessedMaps);
  }

  private void refreshAreaPictureTile(AreaPicture areaPicture) {
    Tile tile = tileCreator.apply(areaPicture);
    areaPicture.setTile(tile);
    areaPicture.setLongitude(tile.getLongitude());
    areaPicture.setLatitude(tile.getLatitude());
  }

  @Transactional
  public AreaPicture save(AreaPicture areaPicture) {
    return mapper.toDomain(jpaRepository.save(mapper.toEntity(areaPicture)));
  }
}
