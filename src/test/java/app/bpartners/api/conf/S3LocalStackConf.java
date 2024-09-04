package app.bpartners.api.conf;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.util.List;

import static app.bpartners.api.endpoint.event.AwsConf.TEST_ENV;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

@Slf4j
public class S3LocalStackConf extends LocalStackConf {
  public static final String BUCKET_NAME = "bpartners";
  public static final String OLD_S3_KEY = "old_s3_key";

  public S3LocalStackConf() {
    super();
  }

  static String testFilePath() {
    return "src/main/resources/files/downloaded.jpeg";
  }

  @Override
  protected String getBucketName(){
    return BUCKET_NAME;
  }

  @Override
  void start() {
    localStackContainer.start();
    S3Client s3Client =
            S3Client.builder()
                    .endpointOverride(localStackContainer.getEndpointOverride(S3))
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
}
