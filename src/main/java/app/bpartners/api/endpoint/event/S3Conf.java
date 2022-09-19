package app.bpartners.api.endpoint.event;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class S3Conf {
  private final String bucketName;

  private final String env;

  public S3Conf(
      @Value("${aws.bucket.name}")
      String bucketName,
      @Value("${env}")
      String env) {
    this.bucketName = bucketName;
    this.env = env;
  }

  public enum S3FileType {
    LOGO,
    INVOICE
  }

}
