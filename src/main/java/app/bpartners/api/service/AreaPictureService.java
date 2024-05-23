package app.bpartners.api.service;


import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.file.BucketComponent;
import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.AreaPictureMapper;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.AreaPictureJpaRepository;
import app.bpartners.api.service.WMS.AreaPictureMapLayerService;
import app.bpartners.api.service.WMS.Tile;
import app.bpartners.api.service.WMS.TileCreator;
import app.bpartners.api.service.WMS.imageSource.WmsImageSource;
import app.bpartners.api.service.aws.S3Service;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class AreaPictureService {
  private final AreaPictureJpaRepository jpaRepository;
  private final AreaPictureMapper mapper;
  // private final FileService fileService;
  private final WmsImageSource wmsImageSource;
  private final TileCreator tileCreator;
  private final AreaPictureMapLayerService mapLayerService;
  //testing
  private final BucketComponent bucketComponent;
  private final S3Conf s3Conf;

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
    var downloadedFile =
        wmsImageSource.downloadImage(
            refreshed.getFilename(), refreshed.getTile(), refreshed.getCurrentLayer());
    bucketComponent.upload(downloadedFile, getAreaPictureKey(refreshed.getIdUser(), refreshed.getIdFileInfo()));
    return save(refreshed);
    /*try {
      var downloadedFileAsBytes = Files.readAllBytes(downloadedFile.toPath());
      fileService.upload(
          refreshed.getIdFileInfo(), AREA_PICTURE, refreshed.getIdUser(), downloadedFileAsBytes);
      return save(refreshed);
    } catch (IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }*/
  }

  private AreaPicture refreshAreaPictureTileAndLayers(AreaPicture areaPicture) {
    refreshAreaPictureTile(areaPicture);
    refreshAreaPictureMapLayers(areaPicture);
    return areaPicture;
  }

  private void refreshAreaPictureMapLayers(AreaPicture areaPicture) {
    var guessedMaps = mapLayerService.getAvailableLayersFrom(areaPicture.getTile());
    if (areaPicture.getCurrentLayer() == null) {
      var latest = mapLayerService.getLatestMostPreciseOrDefault(guessedMaps);
      areaPicture.setCurrentLayer(latest);
    }
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

  private String getAreaPictureKey(String idUser, String fileId) {
    return S3Service.getAreaPictureKey(s3Conf, idUser, fileId);
  }
}
