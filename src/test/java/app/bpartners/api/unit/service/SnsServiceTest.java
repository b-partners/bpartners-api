package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.SnsConf;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.service.SnsService;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

class SnsServiceTest {
  SnsClient snsClientMock = mock();
  SnsConf snsConfMock = mock();
  SnsService subject = new SnsService(snsClientMock, snsConfMock);

  @Test
  void delete_endpoint_arn_ok() {
    when(snsClientMock.deleteEndpoint(any(DeleteEndpointRequest.class)))
        .thenReturn(DeleteEndpointResponse.builder().build());

    assertDoesNotThrow(() -> subject.deleteEndpointArn("arn"));
  }

  @Test
  void delete_endpoint_arn_ko() {
    doThrow(RuntimeException.class)
        .when(snsClientMock)
        .deleteEndpoint(any(DeleteEndpointRequest.class));

    assertThrows(ApiException.class, () -> subject.deleteEndpointArn("arn"));
  }

  @Test
  void create_endpoint_arn_ok() {
    when(snsConfMock.getSnsPlatformArn()).thenReturn("arn:test");
    when(snsClientMock.createPlatformEndpoint(any(CreatePlatformEndpointRequest.class)))
        .thenReturn(CreatePlatformEndpointResponse.builder().endpointArn("arn:endpoint").build());

    String arn = subject.createEndpointArn("deviceToken");
    assertEquals("arn:endpoint", arn);
  }

  @Test
  void create_endpoint_arn_invalid_token() {
    when(snsConfMock.getSnsPlatformArn()).thenReturn("arn:test");
    when(snsClientMock.createPlatformEndpoint(any(CreatePlatformEndpointRequest.class)))
        .thenThrow(InvalidParameterException.builder().build());

    assertThrows(BadRequestException.class, () -> subject.createEndpointArn("badToken"));
  }

  @Test
  void push_notification_without_arn() {
    User user = User.builder().firstName("john").lastName("").build();

    assertDoesNotThrow(() -> subject.pushNotification("Hello", user));
  }

  @Test
  void push_notification_with_arn() {
    User user = User.builder().snsArn("arn:endpoint").firstName("John").lastName("").build();
    when(snsClientMock.publish(any(PublishRequest.class)))
        .thenReturn(PublishResponse.builder().messageId("msg123").build());

    assertDoesNotThrow(() -> subject.pushNotification("Hello", user));
  }
}
