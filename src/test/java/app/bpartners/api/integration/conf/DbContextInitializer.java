package app.bpartners.api.integration.conf;

import lombok.Getter;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static app.bpartners.api.integration.conf.TestUtils.findAvailableTcpPort;

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
        .asCompatibleSubstituteFor("postgres");

    postgresContainer = new PostgreSQLContainer<>(postgis)
        .withDatabaseName("it-db")
        .withUsername("sa")
        .withPassword("sa")
        .withExposedPorts(containerPort);
    postgresContainer.setPortBindings(List.of(String.format("%d:%d", containerPort, localPort)));

    postgresContainer.start();
  }

  public JdbcDatabaseContainer<?> getPostgresContainer() {
    return postgresContainer;
  }
}
