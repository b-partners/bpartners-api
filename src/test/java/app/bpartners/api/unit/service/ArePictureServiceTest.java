package app.bpartners.api.unit.service;

import static app.bpartners.api.model.subscription.SubscriptionConsumptionType.ROOF_ANALYSIS;
import static app.bpartners.api.model.subscription.SubscriptionConsumptionUnit.UNIT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.model.FileInfo;
import app.bpartners.api.model.mapper.AreaPictureMapper;
import app.bpartners.api.model.subscription.SubscriptionConsumptionLog;
import app.bpartners.api.repository.jpa.AreaPictureJpaRepository;
import app.bpartners.api.service.AreaPictureService;
import app.bpartners.api.service.FileService;
import app.bpartners.api.service.WMS.AreaPictureMapLayerService;
import app.bpartners.api.service.WMS.Tile;
import app.bpartners.api.service.WMS.TileCreator;
import app.bpartners.api.service.WMS.imageSource.WmsImageSource;
import app.bpartners.api.service.subscription.SubscriptionService;
import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ArePictureServiceTest {
  SubscriptionService subscriptionServiceMock = mock();
  AreaPictureMapLayerService mapLayerServiceMock = mock();
  TileCreator tileCreatorMock = mock();
  WmsImageSource wmsImageSourceMock = mock();
  FileService fileServiceMock = mock();
  AreaPictureMapper mapper = mock();
  AreaPictureJpaRepository jpaRepositoryMock = mock();

  AreaPictureService subject =
      new AreaPictureService(
          jpaRepositoryMock,
          mapper,
          fileServiceMock,
          wmsImageSourceMock,
          tileCreatorMock,
          mapLayerServiceMock,
          subscriptionServiceMock);

  @Test
  void save_area_picture_and_add_log() {
    var areaPictureMock = mock(AreaPicture.class);
    var tileMock = mock(Tile.class);
    var areaPictureMapLayerMock = mock(AreaPictureMapLayer.class);
    var fileMock = mock(File.class);
    var userId = "userId";
    when(areaPictureMock.getCurrentLayer()).thenReturn(areaPictureMapLayerMock);
    when(areaPictureMock.getFilename()).thenReturn("dummyFilename");
    when(areaPictureMock.getIdUser()).thenReturn(userId);
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
    var subscriptionConsumptionLogCaptor =
        ArgumentCaptor.forClass(SubscriptionConsumptionLog.class);

    var actual = subject.saveArePictureAndLogConsumption(areaPictureMock);

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
            .creationDatetime(subscriptionConsumptionLog.getCreationDatetime())
            .build(),
        subscriptionConsumptionLog);
    assertNotNull(subscriptionConsumptionLog.getId());
    assertNotNull(subscriptionConsumptionLog.getCreationDatetime());
  }
}
