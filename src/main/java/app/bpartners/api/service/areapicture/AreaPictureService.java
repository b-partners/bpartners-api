package app.bpartners.api.service.areapicture;

import static app.bpartners.api.endpoint.rest.model.FileType.AREA_PICTURE;
import static app.bpartners.api.model.subscription.SubscriptionConsumptionType.ROOF_ANALYSIS;
import static app.bpartners.api.model.subscription.SubscriptionConsumptionUnit.UNIT;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.rest.model.ZoomLevel;
import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.exception.ServiceUnavailableException;
import app.bpartners.api.model.mapper.AreaPictureMapper;
import app.bpartners.api.model.subscription.SubscriptionConsumptionLog;
import app.bpartners.api.repository.jpa.AreaPictureJpaRepository;
import app.bpartners.api.repository.jpa.ProspectJpaRepository;
import app.bpartners.api.service.file.FileService;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.wms.AreaPictureMapLayerService;
import app.bpartners.api.service.wms.Tile;
import app.bpartners.api.service.wms.TileCreator;
import app.bpartners.api.service.wms.imageSource.WmsImageSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.AllArgsConstructor;
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
  private final WmsImageSource wmsImageSource;
  private final TileCreator tileCreator;
  private final AreaPictureMapLayerService mapLayerService;
  private final SubscriptionService subscriptionService;
  private final ProspectJpaRepository prospectRepository;
  private final AreaPictureConsumptionValidator areaPictureConsumptionValidator;
  private final AreaPictureZoomValidator areaPictureZoomValidator;

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

  private AreaPicture downloadFromExternalSourceAndSave(AreaPicture areaPicture)
      throws RuntimeException {

    long startRefresh = System.currentTimeMillis();
    var refreshed = refreshAreaPictureTileAndLayers(areaPicture);
    long endRefresh = System.currentTimeMillis();
    log.info("Elapsed time for refreshAreaPictureTileAndLayers: {} ms", endRefresh - startRefresh);

    long startDownload = System.currentTimeMillis();
    var downloadedFile = wmsImageSource.downloadImage(areaPicture);
    long endDownload = System.currentTimeMillis();
    log.info("Elapsed time for downloadImage: {} ms", endDownload - startDownload);

    if (areaPicture.getFilename().contains("ORTHOIMAGERY")) {
      areaPicture.setZoomLevel(ZoomLevel.BUILDING);
      areaPicture.setCurrentLayer(mapLayerService.getDefaultIGNLayer());
    }

    long startUpload = System.currentTimeMillis();
    fileService.upload(
        AREA_PICTURE, refreshed.getIdFileInfo(), refreshed.getIdUser(), downloadedFile);
    long endUpload = System.currentTimeMillis();
    log.info("Elapsed time for fileService.upload: {} ms", endUpload - startUpload);

    long startSave = System.currentTimeMillis();
    var saved = save(refreshed);
    long endSave = System.currentTimeMillis();
    log.info("Elapsed time for save: {} ms", endSave - startSave);
    return saved;
  }

  @Transactional
  public AreaPicture saveAreaPictureAndLogConsumption(AreaPicture picture) {
    areaPictureConsumptionValidator.accept(picture);

    var areaPicture = downloadFromExternalSourceAndSave(picture);
    var usageMetric = 1L;
    var idProspect = picture.getIdProspect();
    var address = areaPicture.getAddress();
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
            .userId(areaPicture.getIdUser())
            .consumptionType(ROOF_ANALYSIS)
            .usageMetric(usageMetric)
            .consumptionUnit(UNIT)
            .comment(comment)
            .creationDatetime(now())
            .build());

    return areaPicture;
  }

  private AreaPicture refreshAreaPictureTileAndLayers(AreaPicture areaPicture) {
    refreshAreaPictureTile(areaPicture);
    refreshAreaPictureMapLayers(areaPicture);
    return areaPicture;
  }

  private void refreshAreaPictureMapLayers(AreaPicture areaPicture) {
    var guessedMaps =
        new LinkedHashSet<>(mapLayerService.getAvailableLayersFrom(areaPicture.getCurrentTile()));
    var fallbackLayers =
        List.of(
            mapLayerService.getRhonePCRSLayer(),
            mapLayerService.getPCRSLayer(),
            mapLayerService.getDefaultIGNLayer());

    if (areaPicture.getCurrentLayer() == null) {
      if (guessedMaps.isEmpty()) {
        areaPicture.setCurrentLayer(mapLayerService.getPCRSLayer());
        guessedMaps.add(areaPicture.getCurrentLayer());
      } else {
        var latest = mapLayerService.getLatestMostPreciseOrDefault(guessedMaps);
        areaPicture.setCurrentLayer(latest);
      }
    }

    guessedMaps.addAll(fallbackLayers);
    areaPicture.setLayers(new ArrayList<>(guessedMaps));
  }

  public List<AreaPictureMapLayer> getMapLayers(Double longitude, Double latitude) {
    var guessedMaps = mapLayerService.getAvailableLayersFrom(longitude, latitude);
    Collections.sort(guessedMaps, Comparator.reverseOrder());
    guessedMaps.addAll(
        List.of(
            mapLayerService.getPCRSLayer(),
            mapLayerService.getRhonePCRSLayer(),
            mapLayerService.getDefaultIGNLayer()));
    return guessedMaps;
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
