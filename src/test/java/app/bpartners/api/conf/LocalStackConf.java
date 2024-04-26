package app.bpartners.api.conf;

import static app.bpartners.api.endpoint.event.AwsConf.TEST_ENV;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

import java.io.File;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
public class LocalStackConf {
  public static final String BUCKET_NAME = "bpartners";
  public static final String OLD_S3_KEY = "old_s3_key";

  static String testFilePath() {
    return "src/main/resources/files/downloaded.jpeg";
  }

  private final LocalStackContainer s3Container =
      new LocalStackContainer(DockerImageName.parse("localstack/localstack:2.0.0"))
          .withServices(S3);

  void start() {
    s3Container.start();
    S3Client s3Client =
        S3Client.builder()
            .endpointOverride(s3Container.getEndpointOverride(S3))
            .region(Region.EU_WEST_3)
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
            .build();

    List<Bucket> buckets = s3Client.listBuckets().buckets();

    if (buckets.isEmpty()) {
      s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
    }

    s3Client.putObject(
        PutObjectRequest.builder()
            .bucket(BUCKET_NAME)
            .key(TEST_ENV + "/accounts/" + OLD_S3_KEY + "/logo/logo.jpeg")
            .build(),
        new File(testFilePath()).toPath());
    s3Client.putObject(
        PutObjectRequest.builder()
            .bucket(BUCKET_NAME)
            .key(TEST_ENV + "/accounts/" + OLD_S3_KEY + "/logo/test.jpeg")
            .build(),
        new File(testFilePath()).toPath());
  }

  void stop() {
    s3Container.stop();
  }

  void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("aws.s3.bucket", () -> BUCKET_NAME);
    registry.add("aws.region", s3Container::getRegion);
    registry.add("aws.endpoint", () -> s3Container.getEndpointOverride(S3));
    registry.add("env", () -> TEST_ENV);
  }
}
