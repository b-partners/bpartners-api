package app.bpartners.api.endpoint.event;

import app.bpartners.api.PojaGenerated;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

@PojaGenerated
@Configuration
public class EventConf {
  private final Region region;

  public EventConf(@Value("${aws.region}") Region region) {
    this.region = region;
  }

  @Bean
  public SqsClient getSqsClient() {
    return SqsClient.builder().region(region).build();
  }
}
