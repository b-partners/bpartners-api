package app.bpartners.api.endpoint.event;

import app.bpartners.api.model.exception.ApiException;
import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.ssm.SsmClient;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

@Configuration
public class EventConf {
  private final Region region;
  private final String s3Endpoint;

  public EventConf(@Value("${aws.region}") String region,
                   @Value("${aws.endpoint}") String s3Endpoint) {
    this.region = Region.of(region);
    this.s3Endpoint = s3Endpoint;
  }

  @Bean
  public SqsClient getSqsClient() {
    return SqsClient.builder()
        .region(region)
        .build();
  }

  @Bean
  public SsmClient getSsmClient() {
    return SsmClient.builder()
        .region(region)
        .build();
  }

  @Bean
  public S3Client getS3Client() {
    try {
      return S3Client.builder()
          .endpointOverride(new URI(s3Endpoint))
          .region(region)
          .build();
    } catch (URISyntaxException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }
}