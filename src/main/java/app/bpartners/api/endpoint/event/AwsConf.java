package app.bpartners.api.endpoint.event;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

@Configuration
public class AwsConf {
  public static final String TEST_ENV = "test";
  private final String env;

  public AwsConf(@Value("${env}") String env) {
    this.env = env;
  }

  @Bean
  public AwsCredentialsProvider awsCredentialsProvider() {
    if (isEnvTest()) {
      return StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"));
    }
    try (var defaultCredentials = DefaultCredentialsProvider.create(); ) {
      return defaultCredentials;
    }
  }

  public boolean isEnvTest() {
    return TEST_ENV.equals(env);
  }
}
