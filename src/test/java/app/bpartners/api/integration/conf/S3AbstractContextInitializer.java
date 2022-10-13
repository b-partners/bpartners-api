package app.bpartners.api.integration.conf;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.core.Field;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

@Slf4j
public abstract class S3AbstractContextInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    PostgreSQLContainer<?> postgresContainer =
        new PostgreSQLContainer<>("postgres:13.2")
            .withDatabaseName("it-db")
            .withUsername("sa")
            .withPassword("sa");
    postgresContainer.start();

    LocalStackContainer s3Container = new LocalStackContainer(DockerImageName.parse(
        "localstack/localstack:0.11.3"))
        .withServices(S3);
    s3Container.start();

    S3Client s3 = S3Client
        .builder()
        .endpointOverride(s3Container.getEndpointOverride(S3))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(s3Container.getAccessKey(), s3Container.getSecretKey())
            )
        )
        .region(Region.of(s3Container.getRegion()))
        .build();

    String bucketName = "bpartners";

    s3.createBucket(CreateBucketRequest.builder()
        .bucket(bucketName)
        .build());

      s3.putObject(PutObjectRequest.builder()
              .bucket(bucketName)
              .contentType(MediaType.IMAGE_JPEG_VALUE)
              .checksumAlgorithm(ChecksumAlgorithm.SHA256)
              .key("dev/accounts/beed1765-5c16-472a-b3f4-5c376ce5db58/logo/test.jpeg")
              .build(),
          RequestBody.fromFile(new File(testFilePath())));

    String flywayTestdataPath = "classpath:/db/testdata";

    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
        applicationContext,
        "server.port=" + this.getServerPort(),
        "aws.cognito.userPool.id=eu-west-3_vq2jlNjq7",
        "aws.eventBridge.bus=dummy",
        "aws.sqs.queueUrl=dummy",
        "spring.datasource.url=" + postgresContainer.getJdbcUrl(),
        "spring.datasource.username=" + postgresContainer.getUsername(),
        "spring.datasource.password=" + postgresContainer.getPassword(),
        "spring.flyway.locations=classpath:/db/migration," + flywayTestdataPath,
        "aws.bucket.name=bpartners",
        "aws.access.key.id=" + s3Container.getAccessKey(),
        "aws.secret.access.key=" + s3Container.getSecretKey(),
        "aws.region=" + s3Container.getRegion(),
        "aws.endpoint.override=" + s3Container.getEndpointOverride(S3),
        "env=dev");
  }

  static String testFilePath() {
    return "src/main/resources/files/downloaded.jpeg";
  }

  public abstract int getServerPort();
}
