package app.bpartners.api.integration.conf;

import static app.bpartners.api.integration.conf.utils.TestUtils.findAvailableTcpPort;

import java.util.List;
import lombok.Getter;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;

public class DbContextInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  private JdbcDatabaseContainer<?> postgresContainer;
  @Getter private final String flywayTestdataPath = "classpath:/db/testdata";

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    int localPort = findAvailableTcpPort();
    int containerPort = 5432;

    postgresContainer =
        new PostgreSQLContainer<>("postgres")
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
