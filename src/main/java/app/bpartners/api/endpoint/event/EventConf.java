package app.bpartners.api.endpoint.event;

import app.bpartners.api.model.exception.ApiException;
import java.net.URI;
import java.net.URISyntaxException;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.ssm.SsmClient;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

@Configuration
public class EventConf {
  private final Region region;
  private final String s3Endpoint;
  private final String sesSource;
  @Getter
  private final String adminEmail; //TODO: set as env variable
  @Getter
  private final String snsTargetArn;

  public EventConf(@Value("${aws.region}") String region,
                   @Value("${aws.endpoint}") String s3Endpoint,
                   @Value("${aws.ses.source}") String sesSource,
                   @Value("${admin.email}") String adminEmail,
                   @Value("${aws.sns.target.arn}") String snsTargetArn) {
    this.sesSource = sesSource;
    this.region = Region.of(region);
    this.s3Endpoint = s3Endpoint;
    this.adminEmail = adminEmail;
    this.snsTargetArn = snsTargetArn;
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

  @Bean
  public SesClient getSesClient() {
    return SesClient.builder()
        .region(region) //Override the localstack default region for live test in localhost
        .build();
  }

  @Bean
  public SnsClient getSnsClient() {
    return SnsClient.builder()
        .region(region) //Override the localstack default region for live test in localhost
        .build();
  }

  public String getSesSource() {
    return this.sesSource;
  }
}