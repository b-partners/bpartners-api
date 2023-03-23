package app.bpartners.api.integration.conf;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgisContainerProvider;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class BridgeAbstractContextInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    JdbcDatabaseContainer postgresContainer =
        new PostgisContainerProvider()
            .newInstance()
            .withDatabaseName("it-db")
            .withUsername("sa")
            .withPassword("sa");
    postgresContainer.start();

    String flywayTestdataPath = "classpath:/db/testdata";
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
        applicationContext,
        "server.port=" + this.getServerPort(),
        "aws.cognito.userPool.id=eu-west-3_vq2jlNjq7",
        "aws.cognito.userPool.domain=dummy",
        "aws.cognito.userPool.clientId=dummy",
        "aws.cognito.userPool.clientSecret=dummy",
        "aws.eventBridge.bus=dummy",
        "aws.sqs.mailboxUrl=dummy",
        "fintecture.base.url=https://api-sandbox.fintecture.com",
        "swan.base.url=https://api.swan.io/sandbox-partner",
        "spring.datasource.url=" + postgresContainer.getJdbcUrl(),
        "spring.datasource.username=" + postgresContainer.getUsername(),
        "spring.datasource.password=" + postgresContainer.getPassword(),
        "spring.flyway.locations=classpath:/db/migration," + flywayTestdataPath);
  }

  public abstract int getServerPort();
}
