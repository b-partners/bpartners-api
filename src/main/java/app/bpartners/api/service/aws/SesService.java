package app.bpartners.api.service.aws;

import app.bpartners.api.endpoint.event.EventConf;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

@Service
@AllArgsConstructor
public class SesService {
  private final EventConf eventConf;
  private final SesClient client;

  public void sendEmail(String destinationEmail, String subject, String htmlBody) {
    client.sendEmail(SendEmailRequest.builder()
        .source(eventConf.getSesSource())
        .destination(Destination.builder()
            .toAddresses(destinationEmail)
            .build())
        .message(Message.builder()
            .subject(Content.builder()
                .charset("utf-8")
                .data(subject)
                .build())
            .body(Body.builder()
                .html(Content.builder()
                    .charset("utf-8")
                    .data(htmlBody)
                    .build())
                .build())
            .build())
        .build());
  }
}
