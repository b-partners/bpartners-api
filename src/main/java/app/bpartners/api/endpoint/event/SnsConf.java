package app.bpartners.api.endpoint.event;

import app.bpartners.api.service.aws.AWSProviderConf;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

@Configuration
public class SnsConf extends AWSProviderConf {
  private final Region region;
  @Getter private final String snsPlatformArn;

  public SnsConf(
      @Value("${sns.platform.arn}") String snsPlatformArn,
      @Value("eu-west-3") Region region,
      AwsCredentialsProvider awsCredentialsProvider) {
    super(awsCredentialsProvider);
    this.snsPlatformArn = snsPlatformArn;
    this.region = region;
  }

  @Bean
  public SnsClient getSnsClient() {
    return SnsClient.builder()
        .credentialsProvider(getAwsCredentialsProvider())
        .region(region)
        .build();
  }
}
