package app.bpartners.api.integration.conf;

import java.util.List;
import lombok.Getter;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static app.bpartners.api.integration.conf.utils.TestUtils.findAvailableTcpPort;

public class DbContextInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  private JdbcDatabaseContainer<?> postgresContainer;
  @Getter
  private final String flywayTestdataPath = "classpath:/db/testdata";

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    int localPort = findAvailableTcpPort();
    int containerPort = 5432;
    DockerImageName postgis = DockerImageName
        .parse("postgis/postgis")
        .asCompatibleSubstituteFor("postgres:13.9");

    postgresContainer = new PostgreSQLContainer<>(postgis)
        .withDatabaseName("it-db")
        .withUsername("sa")
        .withPassword("sa");

    postgresContainer.start();
  }

  public JdbcDatabaseContainer<?> getPostgresContainer() {
    return postgresContainer;
  }
}
