package app.bpartners.api.integration.conf;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class DbEnvContextInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  private final DbContextInitializer dbContextInitializer = new DbContextInitializer();
  private final EnvContextInitializer envContextInitializer = new EnvContextInitializer(dbContextInitializer);

  private static int httpServerPort;

  public static int getHttpServerPort() {
    return httpServerPort;
  }

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    dbContextInitializer.initialize(applicationContext);
    envContextInitializer.initialize(applicationContext);
    httpServerPort = envContextInitializer.getHttpServerPort();
  }
}