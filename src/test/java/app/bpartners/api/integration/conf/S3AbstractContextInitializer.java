package app.bpartners.api.integration.conf;

import java.io.File;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static app.bpartners.api.integration.conf.utils.TestUtils.findAvailableTcpPort;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

@Slf4j
public abstract class S3AbstractContextInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  public static final String FLYWAY_TESTDATA_PATH = "classpath:/db/testdata";
  public static final String BUCKET_NAME = "bpartners";
  public static final String OLD_S3_KEY = "old_s3_key";

  static String testFilePath() {
    return "src/main/resources/files/downloaded.jpeg";
  }

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    int localPort = findAvailableTcpPort();
    int containerPort = 5432;
    JdbcDatabaseContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres")
        .withDatabaseName("it-db")
        .withUsername("sa")
        .withPassword("sa")
        .withExposedPorts(containerPort);
    postgresContainer.setPortBindings(List.of(String.format("%d:%d", containerPort, localPort)));

    postgresContainer.start();

    LocalStackContainer s3Container = new LocalStackContainer(DockerImageName.parse(
        "localstack/localstack:0.11.3"))
        .withServices(S3);
    s3Container.start();

    S3Client s3Client = S3Client.builder()
        .endpointOverride(s3Container.getEndpointOverride(S3))
        .region(Region.of(s3Container.getRegion()))
        .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
        .build();

    s3Client.createBucket(CreateBucketRequest.builder()
            .bucket(BUCKET_NAME)
        .build());
    s3Client.putObject(PutObjectRequest.builder()
        .bucket(BUCKET_NAME)
        .key("dev/accounts/" + OLD_S3_KEY + "/logo/logo.jpeg")
        .build(), new File(testFilePath()).toPath());
    s3Client.putObject(PutObjectRequest.builder()
            .bucket(BUCKET_NAME)
            .key("dev/accounts/" + OLD_S3_KEY + "/logo/test.jpeg")
            .build(), new File(testFilePath()).toPath());

    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
        applicationContext,
        "server.port=" + this.getServerPort(),
        "sns.platform.arn=dummy",
        "aws.cognito.userPool.id=eu-west-3_vq2jlNjq7",
        "aws.cognito.userPool.domain=dummy",
        "aws.cognito.userPool.clientId=dummy",
        "aws.cognito.userPool.clientSecret=dummy",
        "aws.eventBridge.bus=dummy",
        "aws.sqs.mailboxUrl=dummy",
        "google.calendar.apps.name=dummy",
        "google.calendar.client.id=dummy",
        "google.calendar.client.secret=dummy",
        "google.calendar.redirect.uris=https://dummy.com/success",
        "google.sheet.apps.name=dummy",
        "google.sheet.client.id=dummy",
        "google.sheet.client.secret=dummy",
        "google.sheet.redirect.uris=dummy",
        "fintecture.base.url=https://api-sandbox.fintecture.com",
        "swan.base.url=https://api.swan.io/sandbox-partner",
        "spring.datasource.url=" + postgresContainer.getJdbcUrl(),
        "spring.datasource.username=" + postgresContainer.getUsername(),
        "spring.datasource.password=" + postgresContainer.getPassword(),
        "spring.flyway.locations=classpath:/db/migration," + FLYWAY_TESTDATA_PATH,
        "aws.bucket.name=" + BUCKET_NAME,
        "aws.region=" + s3Container.getRegion(),
        "aws.endpoint=" + s3Container.getEndpointOverride(S3),
        "bridge.client.id=dummy",
        "bridge.client.secret=dummy",
        "bridge.base.url=dummy",
        "bridge.version=dummy",
        "ban.base.url=dummy",
        "feature.detector.api.key=dummy",
        "feature.detector.application.name=dummy",
        "expressif.project.token=dummy",
        "env=dev");
  }

  public abstract int getServerPort();
}
