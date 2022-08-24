package app.bpartners.api.endpoint.event;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.ssm.SsmClient;

@Configuration
public class EventConf {
  private final Region region;

  public EventConf(@Value("${aws.region}") String region) {
    this.region = Region.of(region);
  }

  @Bean
  public SqsClient getSqsClient() {
    return SqsClient.builder().region(region).build();
  }

  @Bean
  public SsmClient getSsmClient() {
    return SsmClient.builder().region(region).build();
  }
}