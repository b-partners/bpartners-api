package app.bpartners.api.service.aws;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

@Service
@AllArgsConstructor
@Getter
public abstract class AWSServiceConf {
  private final AwsCredentialsProvider awsCredentialsProvider;
}
