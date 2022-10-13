package app.bpartners.api.endpoint.event;

import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.ssm.SsmClient;

@Configuration
public class EventConf {
  private final Region region;
  private final String accessKeyId;
  private final String secretAccessKey;
  private final String endpointOverride;

  public EventConf(@Value("${aws.region}") String region,
                   @Value("${aws.access.key.id}") String awsKeyId,
                   @Value("${aws.secret.access.key}") String awsKeySecret,
                   @Value("${aws.s3.endpoint.override}") String endpointOverride) {
    this.region = Region.of(region);
    this.accessKeyId = awsKeyId;
    this.secretAccessKey = awsKeySecret;
    this.endpointOverride = endpointOverride;
  }

  @Bean
  public SqsClient getSqsClient() {
    return SqsClient.builder()
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKeyId, secretAccessKey)
        ))
        .region(region).build();
  }

  @Bean
  public SsmClient getSsmClient() {
    return SsmClient.builder()
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKeyId, secretAccessKey)
        ))
        .region(region).build();
  }

  @Bean
  public S3Client getS3Client() {
    try {
      return S3Client.builder()
          .endpointOverride(new URI(endpointOverride))
          .credentialsProvider(StaticCredentialsProvider.create(
              AwsBasicCredentials.create(accessKeyId, secretAccessKey)
          ))
          .region(region).build();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}