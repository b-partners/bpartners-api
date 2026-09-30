package app.bpartners.api.mail;

import java.util.function.UnaryOperator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EmailSubjectPrefixer implements UnaryOperator<String> {
  private static final String PROD_ENV = "prod";

  private final String env;

  public EmailSubjectPrefixer(@Value("${env}") String env) {
    this.env = env;
  }

  @Override
  public String apply(String subject) {
    if (subject == null || env == null || env.isBlank() || PROD_ENV.equalsIgnoreCase(env)) {
      return subject;
    }
    return "[" + env + "] " + subject;
  }
}
