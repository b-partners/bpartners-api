package app.bpartners.api.conf;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

@Slf4j
public class LocalStackConf {
  protected final String BUCKET_NAME="dummy";
  protected final DockerImageName localStackDockerImageName = DockerImageName.parse("localstack/localstack:2.0.0");
  protected final LocalStackContainer localStackContainer;

  public LocalStackConf() {
      this.localStackContainer = new LocalStackContainer(localStackDockerImageName)
              .withServices(S3);
  }

  void start() {
    localStackContainer.start();
  }

  void stop() {
    localStackContainer.stop();
  }

  void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("aws.s3.bucket", this::getBucketName);
    registry.add("aws.region", localStackContainer::getRegion);
    registry.add("aws.endpoint", () -> localStackContainer.getEndpointOverride(S3));
  }

  protected String getBucketName(){
    return BUCKET_NAME;
  }
}
