package app.bpartners.api.service.areapicture;

import static app.bpartners.api.endpoint.rest.model.FileType.AREA_PICTURE;
import static app.bpartners.api.model.subscription.SubscriptionConsumptionType.ROOF_ANALYSIS;
import static app.bpartners.api.model.subscription.SubscriptionConsumptionUnit.UNIT;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.rest.model.AreaPictureDetails;
import app.bpartners.api.file.FileDownloaderImpl;
import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.AreaPictureMapper;
import app.bpartners.api.model.subscription.SubscriptionConsumptionLog;
import app.bpartners.api.repository.jpa.AreaPictureJpaRepository;
import app.bpartners.api.repository.jpa.ProspectJpaRepository;
import app.bpartners.api.service.file.FileService;
import app.bpartners.api.service.geodata.ImageryService;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.wms.AreaPictureMapLayerService;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@AllArgsConstructor
public class AreaPictureService {
  private final AreaPictureJpaRepository jpaRepository;
  private final AreaPictureMapper mapper;
  private final FileService fileService;
  private final AreaPictureMapLayerService mapLayerService;
  private final SubscriptionService subscriptionService;
  private final ProspectJpaRepository prospectRepository;
  private final AreaPictureConsumptionValidator areaPictureConsumptionValidator;
  private final ImageryService imageryService;
  private final FileDownloaderImpl fileDownloader;

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

  @SneakyThrows
  @Transactional
  public AreaPictureDetails downloadFromExternalSource(AreaPicture areaPicture) {
    var areaPictureDetails =
        imageryService.downloadFromGeodataSource(mapper.toCrupdatedAreaPictureDetails(areaPicture));
    areaPictureDetails.setId(areaPicture.getId());
    areaPictureDetails.setProspectId(areaPicture.getIdProspect());
    log.info("Retrieved areaPictureDetails = {}", areaPictureDetails);
    var refreshed =
        mapper.toDomain(
            areaPictureDetails,
            areaPicture.getId(),
            areaPicture.getIdUser(),
            areaPicture.getIdProspect());
    areaPicture = refreshed;
    log.info("Refreshed supposed to be saved = {}", refreshed);
    String filePresignedUrl =
        Objects.requireNonNull(areaPictureDetails.getImagePresignedUrl()).getValue();
    assert filePresignedUrl != null;
    var downloadedFile = fileDownloader.get(areaPicture.getFilename(), new URI(filePresignedUrl));
    fileService.upload(
        AREA_PICTURE, refreshed.getIdFileInfo(), refreshed.getIdUser(), downloadedFile);
    save(areaPicture);
    saveLogConsumption(areaPicture);
    return areaPictureDetails;
  }

  public AreaPicture saveLogConsumption(AreaPicture picture) {
    areaPictureConsumptionValidator.accept(picture);

    var usageMetric = 1L;
    var idProspect = picture.getIdProspect();
    var address = picture.getAddress();
    var comment = "Adresse : " + address;

    // TODO: Bad ! Only areaPicture must be returned done here
    if (idProspect != null) {
      var optionalProspect = prospectRepository.findById(idProspect);
      if (optionalProspect.isPresent()) {
        var prospect = optionalProspect.get();
        var prospectName =
            prospect.getOldName() == null ? prospect.getNewName() : prospect.getOldName();
        comment += " - Prospect : " + prospectName;
      }
    }

    // TODO: Bad ! Only areaPicture must be returned done here
    subscriptionService.addConsumption(
        SubscriptionConsumptionLog.builder()
            .id(randomUUID().toString())
            .userId(picture.getIdUser())
            .consumptionType(ROOF_ANALYSIS)
            .usageMetric(usageMetric)
            .consumptionUnit(UNIT)
            .comment(comment)
            .creationDatetime(now())
            .build());

    return picture;
  }

  public List<AreaPictureMapLayer> getMapLayers(Double longitude, Double latitude)
      throws IOException, InterruptedException, URISyntaxException {
    var guessedMaps = mapLayerService.getAvailableLayersFrom(longitude, latitude);
    //    TODO : mapLayerService already return the correct availableLayersFrom coordinates ordered
    // by latest to oldest
    //    Collections.sort(guessedMaps, Comparator.reverseOrder());
    //    guessedMaps.addAll(
    //        List.of(
    //            mapLayerService.getPCRSLayer(),
    //            mapLayerService.getRhonePCRSLayer(),
    //            mapLayerService.getDefaultIGNLayer(),
    //            mapLayerService.getAirbusLayer()));
    return guessedMaps;
  }

  @Transactional
  public AreaPicture save(AreaPicture areaPicture) {
    return mapper.toDomain(jpaRepository.save(mapper.toEntity(areaPicture)));
  }
}
