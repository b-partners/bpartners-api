package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.SnsConf;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointRequest;
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointResponse;
import software.amazon.awssdk.services.sns.model.DeleteEndpointRequest;
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
  private SnsConf snsConf;

  public void deleteEndpointArn(String arn) {
    try {
      snsClient.deleteEndpoint(DeleteEndpointRequest.builder()
          .endpointArn(arn)
          .build());
    } catch (Exception e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public String createEndpointArn(String deviceToken) {
    try {
      CreatePlatformEndpointResponse platformEndpoint =
          snsClient.createPlatformEndpoint(CreatePlatformEndpointRequest.builder()
              .token(deviceToken)
              .platformApplicationArn(snsConf.getSnsPlatformArn())
              .build());
      return platformEndpoint.endpointArn();
    } catch (InvalidParameterException | NotFoundException e) {
      throw new BadRequestException("Invalid provided device token " + deviceToken);
    } catch (Exception e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public void pushNotification(String message, User user) {
    String snsArn = user.getSnsArn();
    if (snsArn == null) {
      log.warn(
          "[FAILED] Mobile notification with message content [{}]"
              + " not sent to {} because SNS ARN is null",
          message, user.getName());
    } else {
      try {
        PublishResponse publishResponse = snsClient.publish(PublishRequest.builder()
            .targetArn(snsArn)
            .message(message)
            .build());
        log.info("Notifications pushed with messageId=" + publishResponse.messageId());
      } catch (Exception e) {
        log.warn(
            "[FAILED] Mobile notification with message content [{}]"
                + " not sent to {} because {}}",
            message, user.getName(), e.getMessage());
      }
    }
  }
}