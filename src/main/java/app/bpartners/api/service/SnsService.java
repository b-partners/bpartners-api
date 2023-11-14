package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.EventConf;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointRequest;
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointResponse;
import software.amazon.awssdk.services.sns.model.InvalidParameterException;
import software.amazon.awssdk.services.sns.model.NotFoundException;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

@Service
@AllArgsConstructor
@Slf4j
public class SnsService {
  private SnsClient snsClient;
  private EventConf eventConf;

  public String createEndpointArn(String deviceToken) {
    try {
      CreatePlatformEndpointResponse platformEndpoint =
          snsClient.createPlatformEndpoint(CreatePlatformEndpointRequest.builder()
              .token(deviceToken)
              .platformApplicationArn(eventConf.getSnsPlatformArn())
              .build());
      return platformEndpoint.endpointArn();
    } catch (InvalidParameterException | NotFoundException e) {
      throw new BadRequestException("Invalid provided device token " + deviceToken);
    } catch (Exception e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public void pushNotification(String message, User user) {
    PublishResponse publishResponse = snsClient.publish(PublishRequest.builder()
        .targetArn(user.getSnsArn())
        .message(message)
        .build());
    log.info("Notifications pushed with messageId=" + publishResponse.messageId());
  }
}
