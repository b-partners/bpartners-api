package app.bpartners.api.service;

import static app.bpartners.api.endpoint.rest.model.FileType.AREA_PICTURE;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.util.stream.Collectors.toUnmodifiableList;

import app.bpartners.api.file.FileDownloader;
import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.AreaPictureMapper;
import app.bpartners.api.repository.jpa.AreaPictureJpaRepository;
import app.bpartners.api.service.WMS.MapLayerGuesser;
import app.bpartners.api.service.WMS.TileCreator;
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
  private final TileCreator tileCreator;
  private final MapLayerGuesser mapLayerGuesser;

  public List<AreaPicture> findAllBy(String userId, String address, String filename) {
    return jpaRepository
        .findAllByIdUserAndAddressContainingIgnoreCaseAndFilenameContainingIgnoreCase(
            userId, address, filename)
        .stream()
        .map(mapper::toDomain)
        .collect(toUnmodifiableList());
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
    domain.setLayers(mapLayerGuesser.apply(domain));
    return domain;
  }

  @Transactional
  public byte[] downloadFromExternalSourceAndSave(AreaPicture areaPicture) throws RuntimeException {
    var uri = wmsUrlGetter.apply(tileCreator.apply(areaPicture));
    var downloadedFile = fileDownloader.apply(areaPicture.getFilename(), uri);
    try {
      var downloadedFileAsBytes = Files.readAllBytes(downloadedFile.toPath());
      fileService.upload(
          areaPicture.getIdFileInfo(),
          AREA_PICTURE,
          areaPicture.getIdUser(),
          downloadedFileAsBytes);
      save(areaPicture);
      return downloadedFileAsBytes;
    } catch (IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  @Transactional
  public AreaPicture save(AreaPicture areaPicture) {
    return mapper.toDomain(jpaRepository.save(mapper.toEntity(areaPicture)));
  }
}
