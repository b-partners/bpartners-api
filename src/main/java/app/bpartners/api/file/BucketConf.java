package app.bpartners.api.file;

import app.bpartners.api.PojaGenerated;
import app.bpartners.api.service.aws.AWSProviderConf;
import java.net.URI;
import lombok.Getter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

@PojaGenerated
@SuppressWarnings("all")
@Configuration
public class BucketConf {

  @Getter private final String bucketName;
  @Getter private final S3TransferManager s3TransferManager;
  @Getter private final S3Presigner s3Presigner;
  @Getter private final S3Client s3Client;
  private final AWSProviderConf awsProviderConf;

  @SneakyThrows
  public BucketConf(
      @Value("${aws.region}") String regionString,
      @Value("${aws.s3.bucket}") String bucketName,
      @Value("${aws.endpoint.override}") String endpointOverride,
      AWSProviderConf awsProviderConf) {
    this.awsProviderConf = awsProviderConf;
    var endpointOverrideURI = endpointOverride == null ? null : URI.create(endpointOverride);
    var region = Region.of(regionString);
    this.s3TransferManager = configureTransferManager(endpointOverrideURI, region);
    this.s3Presigner = configurePresigner(region);
    this.s3Client = configureClient(endpointOverrideURI, region);
    this.bucketName = bucketName;
  }

  private S3TransferManager configureTransferManager(URI endpointOverride, Region region) {
    S3CrtAsyncClientBuilder clientBuilder = S3AsyncClient.crtBuilder()
        .region(region)
        .credentialsProvider(awsProviderConf.getAwsCredentialsProvider());
    if (endpointOverride != null) clientBuilder.endpointOverride(endpointOverride);
    return S3TransferManager.builder().s3Client(clientBuilder.build()).build();
  }

  private S3Presigner configurePresigner(Region region) {
    return S3Presigner.builder().region(region)
        .credentialsProvider(awsProviderConf.getAwsCredentialsProvider())
        .build();
  }

  private S3Client configureClient(URI endpointOverride, Region region) {
    S3ClientBuilder clientBuilder = S3Client.builder()
        .region(region)
        .credentialsProvider(awsProviderConf.getAwsCredentialsProvider());
    if (endpointOverride != null) clientBuilder.endpointOverride(endpointOverride);
    return clientBuilder.build();
  }
}
