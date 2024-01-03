package app.bpartners.api.endpoint.event;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

@Configuration
public class SesConf {
  private final Region region;
  @Getter private final String sesSource;
  @Getter private final String adminEmail;

  public SesConf(
      @Value("${aws.ses.source}") String sesSource,
      @Value("${admin.email}") String adminEmail,
      @Value("eu-west-3") Region region) {
    this.sesSource = sesSource;
    this.adminEmail = adminEmail;
    this.region = region;
  }
}
