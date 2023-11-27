package app.bpartners.api.endpoint.event;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

@Configuration
public class SnsConf {
  private final Region region;
  @Getter
  private final String snsPlatformArn;

  public SnsConf(@Value("${sns.platform.arn}")String snsPlatformArn,
                 @Value("eu-west-3") Region region) {
    this.snsPlatformArn = snsPlatformArn;
    this.region = region;
  }

  @Bean
  public SnsClient getSnsClient(){
    return SnsClient.builder()
        .region(region)
        .build();
  }
}
