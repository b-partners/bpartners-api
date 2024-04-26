package app.bpartners.api.endpoint.event;

import static app.bpartners.api.endpoint.event.AwsConf.TEST_ENV;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static software.amazon.awssdk.services.s3.model.ChecksumAlgorithm.SHA256;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.service.aws.AWSServiceConf;
import java.net.URI;
import java.net.URISyntaxException;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@Getter
public class S3Conf extends AWSServiceConf {
  private final String s3Endpoint;
  private final String bucketName;
  private final Region region;
  private final String env;

  public S3Conf(
      @Value("${aws.bucket.name}") String bucketName,
      @Value("${env}") String env,
      @Value("${aws.endpoint}") String s3Endpoint,
      @Value("eu-west-3") Region region,
      AwsCredentialsProvider awsCredentialsProvider) {
    super(awsCredentialsProvider);
    this.bucketName = bucketName;
    this.env = env;
    this.s3Endpoint = s3Endpoint;
    this.region = region;
  }

  @Bean
  public S3Client getS3Client() {
    try {
      return S3Client.builder()
          .credentialsProvider(getAwsCredentialsProvider())
          .endpointOverride(new URI(s3Endpoint))
          .region(region)
          .build();
    } catch (URISyntaxException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  @Bean
  public S3Presigner getS3Presigner() {
    return S3Presigner.builder()
        .credentialsProvider(getAwsCredentialsProvider())
        .region(region)
        .build();
  }

  public ChecksumAlgorithm getDefaultChecksumAlgorithm() {
    return TEST_ENV.equals(env) ? null : SHA256;
  }
}
