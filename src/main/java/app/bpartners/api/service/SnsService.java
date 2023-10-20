package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.EventConf;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

@Service
@AllArgsConstructor
@Slf4j
public class SnsService {
  private SnsClient snsClient;
  private EventConf eventConf;

  public void pushNotification(String message) {
    PublishResponse publishResponse = snsClient.publish(PublishRequest.builder()
        .targetArn(eventConf.getSnsTargetArn())
        .message(message)
        .build());
    log.info("Notifications pushed with messageId=" + publishResponse.messageId());
  }
}
