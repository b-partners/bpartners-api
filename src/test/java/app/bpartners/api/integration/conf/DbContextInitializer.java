package app.bpartners.api.integration.conf;

import java.util.List;
import lombok.Getter;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static app.bpartners.api.integration.conf.utils.TestUtils.findAvailableTcpPort;
import static java.lang.Runtime.getRuntime;

public class DbContextInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  private JdbcDatabaseContainer<?> postgresContainer;
  @Getter
  private final String flywayTestdataPath = "classpath:/db/testdata";

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {

    postgresContainer = new PostgreSQLContainer<>("postgres")
        .withDatabaseName("it-db")
        .withUsername("sa")
        .withPassword("sa");

    postgresContainer.start();
    getRuntime()
        .addShutdownHook(new Thread(postgresContainer::stop));
  }

  public JdbcDatabaseContainer<?> getPostgresContainer() {
    return postgresContainer;
  }
}
