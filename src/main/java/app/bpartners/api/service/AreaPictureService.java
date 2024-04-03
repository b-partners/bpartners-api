package app.bpartners.api.service;

import static app.bpartners.api.endpoint.rest.model.FileType.AREA_PICTURE;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.util.stream.Collectors.toUnmodifiableList;

import app.bpartners.api.file.FileDownloader;
import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.AreaPictureMapper;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.ban.model.GeoPosition;
import app.bpartners.api.repository.jpa.AreaPictureJpaRepository;
import app.bpartners.api.service.WMS.Tile;
import app.bpartners.api.service.WMS.WmsUrlGetter;
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
  private final WmsUrlGetter wmsUrlGetter;
  private final BanApi banApi;

  public List<AreaPicture> findAllBy(String userId, String address, String filename) {
    return jpaRepository
        .findAllByIdUserAndAddressContainingIgnoreCaseAndFilenameContainingIgnoreCase(
            userId, address, filename)
        .stream()
        .map(mapper::toDomain)
        .collect(toUnmodifiableList());
  }

  public AreaPicture findBy(String userId, String id) {
    return mapper.toDomain(
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
  }

  @Transactional
  public byte[] downloadFromExternalSourceAndSave(AreaPicture areaPicture) throws RuntimeException {
    var refreshedAreaPicture = computeGeoPosition(areaPicture);
    var uri = wmsUrlGetter.apply(Tile.from(refreshedAreaPicture));
    var downloadedFile = fileDownloader.apply(refreshedAreaPicture.getFilename(), uri);
    try {
      var downloadedFileAsBytes = Files.readAllBytes(downloadedFile.toPath());
      fileService.upload(
          refreshedAreaPicture.getIdFileInfo(),
          AREA_PICTURE,
          refreshedAreaPicture.getIdUser(),
          downloadedFileAsBytes);
      save(refreshedAreaPicture);
      return downloadedFileAsBytes;
    } catch (IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  @Transactional
  public AreaPicture save(AreaPicture areaPicture) {
    return mapper.toDomain(jpaRepository.save(mapper.toEntity(areaPicture)));
  }

  public AreaPicture computeGeoPosition(AreaPicture areaPicture) {
    GeoPosition geoPosition = banApi.fSearch(areaPicture.getAddress());

    areaPicture.setLongitude(geoPosition.getCoordinates().getLongitude());
    areaPicture.setLatitude(geoPosition.getCoordinates().getLatitude());

    return areaPicture;
  }
}
