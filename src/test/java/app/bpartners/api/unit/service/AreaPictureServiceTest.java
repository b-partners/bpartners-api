package app.bpartners.api.unit.service;

import static app.bpartners.api.model.subscription.SubscriptionConsumptionType.IMAGE_ACCESS;
import static app.bpartners.api.model.subscription.SubscriptionConsumptionUnit.UNIT;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.rest.model.AreaPictureDetails;
import app.bpartners.api.endpoint.rest.model.GeoPosition;
import app.bpartners.api.endpoint.rest.model.PreSignedURL;
import app.bpartners.api.file.FileDownloaderImpl;
import app.bpartners.api.integration.conf.MockedThirdParties;
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
import app.bpartners.api.service.geodata.ImageryService;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.wms.AreaPictureMapLayerService;
import java.io.File;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@Slf4j
class AreaPictureServiceTest extends MockedThirdParties {
  SubscriptionService subscriptionServiceMock = mock();
  AreaPictureMapLayerService mapLayerServiceMock = mock();
  FileService fileServiceMock = mock();
  AreaPictureMapper mapper = mock();
  AreaPictureJpaRepository jpaRepositoryMock = mock();
  ProspectJpaRepository prospectJpaRepositoryMock = mock();
  AreaPictureConsumptionValidator consumptionValidatorMock = mock();
  AreaPictureZoomValidator areaPictureZoomValidatorMock = mock();
  FileDownloaderImpl fileDownloaderMock = mock();
  ImageryService imageryServiceMock = mock();

  AreaPictureService subject =
      new AreaPictureService(
          jpaRepositoryMock,
          mapper,
          fileServiceMock,
          mapLayerServiceMock,
          subscriptionServiceMock,
          prospectJpaRepositoryMock,
          consumptionValidatorMock,
          imageryServiceMock,
          fileDownloaderMock);

  @Test
  void save_area_picture_and_add_image_access_log_not_roof_analysis() {
    doNothing().when(areaPictureZoomValidatorMock).accept(any());
    var areaPictureMock = mock(AreaPicture.class);
    var geoPositionMock = mock(GeoPosition.class);
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
    when(mapLayerServiceMock.getAvailableLayersFrom(geoPositionMock))
        .thenReturn(List.of(areaPictureMapLayerMock));
    when(imageryServiceMock.downloadFromGeodataSource(any()))
        .thenReturn(
            new AreaPictureDetails()
                .fileId("fileId")
                .imagePresignedUrl(new PreSignedURL().value("https://dummy-presigned-url")));
    when(mapper.toDomain(any(), any(), any(), any())).thenReturn(mock(AreaPicture.class));
    when(fileServiceMock.upload(any(), any(), any(), any())).thenReturn(mock(FileInfo.class));
    when(fileDownloaderMock.get(any(), any())).thenReturn(fileMock);
    when(jpaRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(mapper.toDomain(any())).thenReturn(areaPictureMock);
    when(subscriptionServiceMock.addConsumption(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    var subscriptionConsumptionLogCaptor =
        ArgumentCaptor.forClass(SubscriptionConsumptionLog.class);

    var actual = subject.saveLogConsumption(areaPictureMock);

    verify(subscriptionServiceMock, times(1))
        .addConsumption(subscriptionConsumptionLogCaptor.capture());
    var subscriptionConsumptionLog = subscriptionConsumptionLogCaptor.getValue();
    assertEquals(areaPictureMock, actual);
    assertEquals(
        SubscriptionConsumptionLog.builder()
            .id(subscriptionConsumptionLog.getId())
            .userId(areaPictureMock.getIdUser())
            .consumptionType(IMAGE_ACCESS)
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
  @Disabled
  void return_service_unavailable() {
    var areaPictureMock = mock(AreaPicture.class);
    var randomAddress = "random address " + randomUUID();

    when(areaPictureMock.getAddress()).thenReturn(randomAddress);
    when(areaPictureMock.getFilename()).thenReturn("dummyFilename");
    doNothing().when(consumptionValidatorMock).accept(areaPictureMock);
    when(fileServiceMock.upload(any(), any(), any(), any())).thenReturn(mock(FileInfo.class));
    when(mapper.toEntity(areaPictureMock)).thenReturn(mock());
    when(jpaRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(mapper.toDomain(any())).thenReturn(areaPictureMock);
    doThrow(ServiceUnavailableException.class)
        .when(areaPictureZoomValidatorMock)
        .accept(areaPictureMock);

    var actualException =
        assertThrows(
            ServiceUnavailableException.class, () -> subject.saveLogConsumption(areaPictureMock));

    assertEquals(
        "Address or zone " + randomAddress + " temporarily unavailable",
        actualException.getMessage());
  }

  @Test
  @Disabled
  void return_not_implemented() {
    var areaPictureMock = mock(AreaPicture.class);
    var randomAddress = "random address " + randomUUID();

    when(areaPictureMock.getAddress()).thenReturn(randomAddress);
    when(areaPictureMock.getFilename()).thenReturn("dummyFilename");
    doNothing().when(consumptionValidatorMock).accept(areaPictureMock);
    when(fileServiceMock.upload(any(), any(), any(), any())).thenReturn(mock(FileInfo.class));
    when(mapper.toEntity(areaPictureMock)).thenReturn(mock());
    when(jpaRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(mapper.toDomain(any())).thenReturn(areaPictureMock);
    doThrow(NotImplementedException.class)
        .when(areaPictureZoomValidatorMock)
        .accept(areaPictureMock);

    var actualException =
        assertThrows(
            NotImplementedException.class, () -> subject.saveLogConsumption(areaPictureMock));

    assertEquals(
        "Address or zone " + randomAddress + " not yet supported", actualException.getMessage());
  }
}
