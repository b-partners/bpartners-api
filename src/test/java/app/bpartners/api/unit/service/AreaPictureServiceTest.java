package app.bpartners.api.unit.service;

import static app.bpartners.api.model.subscription.SubscriptionConsumptionType.ROOF_ANALYSIS;
import static app.bpartners.api.model.subscription.SubscriptionConsumptionUnit.UNIT;
import static app.bpartners.api.service.wms.imageSource.WmsImageSourceFacadeIT.ignLayer;
import static app.bpartners.api.service.wms.imageSource.WmsImageSourceFacadeIT.pcrsLayer;
import static app.bpartners.api.service.wms.imageSource.WmsImageSourceFacadeIT.rhonePCRSLayer;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.model.FileInfo;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.exception.ServiceUnavailableException;
import app.bpartners.api.model.mapper.AreaPictureMapper;
import app.bpartners.api.model.subscription.SubscriptionConsumptionLog;
import app.bpartners.api.repository.jpa.AreaPictureJpaRepository;
import app.bpartners.api.repository.jpa.ProspectJpaRepository;
import app.bpartners.api.repository.jpa.model.HProspect;
import app.bpartners.api.service.areapicture.AreaPictureConsumptionValidator;
import app.bpartners.api.service.areapicture.AreaPictureService;
import app.bpartners.api.service.areapicture.AreaPictureZoomValidator;
import app.bpartners.api.service.file.FileService;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.wms.AreaPictureMapLayerService;
import app.bpartners.api.service.wms.Tile;
import app.bpartners.api.service.wms.TileCreator;
import app.bpartners.api.service.wms.imageSource.WmsImageSource;
import java.io.File;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AreaPictureServiceTest {
  SubscriptionService subscriptionServiceMock = mock();
  AreaPictureMapLayerService mapLayerServiceMock = mock();
  TileCreator tileCreatorMock = mock();
  WmsImageSource wmsImageSourceMock = mock();
  FileService fileServiceMock = mock();
  AreaPictureMapper mapper = mock();
  AreaPictureJpaRepository jpaRepositoryMock = mock();
  ProspectJpaRepository prospectJpaRepositoryMock = mock();
  AreaPictureConsumptionValidator consumptionValidatorMock = mock();
  AreaPictureZoomValidator areaPictureZoomValidatorMock = mock();

  AreaPictureService subject =
      new AreaPictureService(
          jpaRepositoryMock,
          mapper,
          fileServiceMock,
          wmsImageSourceMock,
          tileCreatorMock,
          mapLayerServiceMock,
          subscriptionServiceMock,
          prospectJpaRepositoryMock,
          consumptionValidatorMock,
          areaPictureZoomValidatorMock);

  @Test
  void save_area_picture_and_add_log() {
    doNothing().when(areaPictureZoomValidatorMock).accept(any());
    var areaPictureMock = mock(AreaPicture.class);
    var tileMock = mock(Tile.class);
    var areaPictureMapLayerMock = mock(AreaPictureMapLayer.class);
    var fileMock = mock(File.class);
    var prospectMock = mock(HProspect.class);
    var userId = "userId";
    var prospectAddress = "prospectAddress";
    var prospectId = "prospectId";
    var prospectName = "prospectOldName";
    doNothing().when(consumptionValidatorMock).accept(areaPictureMock);
    when(areaPictureMock.getCurrentLayer()).thenReturn(areaPictureMapLayerMock);
    when(areaPictureMock.getFilename()).thenReturn("dummyFilename");
    when(areaPictureMock.getIdUser()).thenReturn(userId);
    when(areaPictureMock.getAddress()).thenReturn(prospectAddress);
    when(areaPictureMock.getIdProspect()).thenReturn(prospectId);
    when(prospectMock.getOldName()).thenReturn(prospectName);
    when(prospectJpaRepositoryMock.findById(prospectId)).thenReturn(Optional.of(prospectMock));
    when(tileCreatorMock.apply(areaPictureMock)).thenReturn(tileMock);
    when(mapLayerServiceMock.getAvailableLayersFrom(tileMock))
        .thenReturn(List.of(areaPictureMapLayerMock));
    when(wmsImageSourceMock.downloadImage(areaPictureMock)).thenReturn(fileMock);
    when(fileServiceMock.upload(any(), any(), any(), any())).thenReturn(mock(FileInfo.class));
    when(jpaRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(mapper.toDomain(any())).thenReturn(areaPictureMock);
    when(subscriptionServiceMock.addConsumption(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(mapLayerServiceMock.getPCRSLayer()).thenReturn(pcrsLayer());
    when(mapLayerServiceMock.getRhonePCRSLayer()).thenReturn(rhonePCRSLayer());
    when(mapLayerServiceMock.getDefaultIGNLayer()).thenReturn(ignLayer());
    var subscriptionConsumptionLogCaptor =
        ArgumentCaptor.forClass(SubscriptionConsumptionLog.class);

    var actual = subject.saveAreaPictureAndLogConsumption(areaPictureMock);

    verify(subscriptionServiceMock, times(1))
        .addConsumption(subscriptionConsumptionLogCaptor.capture());
    var subscriptionConsumptionLog = subscriptionConsumptionLogCaptor.getValue();
    assertEquals(areaPictureMock, actual);
    assertEquals(
        SubscriptionConsumptionLog.builder()
            .id(subscriptionConsumptionLog.getId())
            .userId(areaPictureMock.getIdUser())
            .consumptionType(ROOF_ANALYSIS)
            .consumptionUnit(UNIT)
            .usageMetric(1L)
            .comment("Adresse : " + prospectAddress + " - Prospect : " + prospectName)
            .creationDatetime(subscriptionConsumptionLog.getCreationDatetime())
            .build(),
        subscriptionConsumptionLog);
    assertNotNull(subscriptionConsumptionLog.getId());
    assertNotNull(subscriptionConsumptionLog.getCreationDatetime());
  }

  @Test
  void return_service_unavailable() {
    var areaPictureMock = mock(AreaPicture.class);
    var downloadedFileMock = mock(File.class);
    var randomAddress = "random address " + randomUUID();

    when(areaPictureMock.getAddress()).thenReturn(randomAddress);
    when(areaPictureMock.getFilename()).thenReturn("dummyFilename");
    doNothing().when(consumptionValidatorMock).accept(areaPictureMock);
    when(wmsImageSourceMock.downloadImage(areaPictureMock)).thenReturn(downloadedFileMock);
    when(fileServiceMock.upload(any(), any(), any(), any())).thenReturn(mock(FileInfo.class));
    when(mapper.toEntity(areaPictureMock)).thenReturn(mock());
    when(jpaRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(mapper.toDomain(any())).thenReturn(areaPictureMock);
    doThrow(ServiceUnavailableException.class)
        .when(areaPictureZoomValidatorMock)
        .accept(areaPictureMock);
    when(mapLayerServiceMock.getPCRSLayer()).thenReturn(pcrsLayer());
    when(mapLayerServiceMock.getRhonePCRSLayer()).thenReturn(rhonePCRSLayer());
    when(mapLayerServiceMock.getDefaultIGNLayer()).thenReturn(ignLayer());

    var actualException =
        assertThrows(
            ServiceUnavailableException.class,
            () -> subject.saveAreaPictureAndLogConsumption(areaPictureMock));

    assertEquals(
        "Address or zone " + randomAddress + " temporarily unavailable",
        actualException.getMessage());
  }

  @Test
  void return_not_implemented() {
    var areaPictureMock = mock(AreaPicture.class);
    var downloadedFileMock = mock(File.class);
    var randomAddress = "random address " + randomUUID();

    when(areaPictureMock.getAddress()).thenReturn(randomAddress);
    when(areaPictureMock.getFilename()).thenReturn("dummyFilename");
    doNothing().when(consumptionValidatorMock).accept(areaPictureMock);
    when(wmsImageSourceMock.downloadImage(areaPictureMock)).thenReturn(downloadedFileMock);
    when(fileServiceMock.upload(any(), any(), any(), any())).thenReturn(mock(FileInfo.class));
    when(mapper.toEntity(areaPictureMock)).thenReturn(mock());
    when(jpaRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(mapper.toDomain(any())).thenReturn(areaPictureMock);
    doThrow(NotImplementedException.class)
        .when(areaPictureZoomValidatorMock)
        .accept(areaPictureMock);
    when(mapLayerServiceMock.getPCRSLayer()).thenReturn(pcrsLayer());
    when(mapLayerServiceMock.getRhonePCRSLayer()).thenReturn(rhonePCRSLayer());
    when(mapLayerServiceMock.getDefaultIGNLayer()).thenReturn(ignLayer());

    var actualException =
        assertThrows(
            NotImplementedException.class,
            () -> subject.saveAreaPictureAndLogConsumption(areaPictureMock));

    assertEquals(
        "Address or zone " + randomAddress + " not yet supported", actualException.getMessage());
  }
}
