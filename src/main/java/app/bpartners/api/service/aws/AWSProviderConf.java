package app.bpartners.api.service.aws;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

@Configuration
@AllArgsConstructor
@Getter
public abstract class AWSProviderConf {
  private final AwsCredentialsProvider awsCredentialsProvider;
}
