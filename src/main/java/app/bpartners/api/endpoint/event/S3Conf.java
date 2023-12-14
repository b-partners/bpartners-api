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
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

@Configuration
@Getter
public class S3Conf {
  private final String s3Endpoint;
  private final String bucketName;
  private final Region region;

  private final String env;

  public S3Conf(
      @Value("${aws.bucket.name}")
      String bucketName,
      @Value("${env}") String env, @Value("${aws.endpoint}") String s3Endpoint,
      @Value("eu-west-3")Region region) {
    this.bucketName = bucketName;
    this.env = env;
    this.s3Endpoint = s3Endpoint;
    this.region = region;
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
  public S3Presigner getS3Presigner(){
    return S3Presigner.builder()
        .region(region)
        .build();
  }
}