package app.bpartners.api.integration.conf;

import static app.bpartners.api.integration.conf.utils.TestUtils.findAvailableTcpPort;

import java.util.List;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class BanAbstractContextInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    int localPort = findAvailableTcpPort();
    int containerPort = 5432;

    JdbcDatabaseContainer<?> postgresContainer =
        new PostgreSQLContainer<>()
            .withDatabaseName("it-db")
            .withUsername("sa")
            .withPassword("sa")
            .withExposedPorts(containerPort);
    postgresContainer.setPortBindings(List.of(String.format("%d:%d", containerPort, localPort)));

    postgresContainer.start();

    String flywayTestdataPath = "classpath:/db/testdata";
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
        applicationContext,
        "server.port=" + this.getServerPort(),
        "sns.platform.arn=dummy",
        "bridge.client.id=dummy",
        "bridge.client.secret=dummy",
        "bridge.base.url=dummy",
        "bridge.version=dummy",
        "google.calendar.apps.name=dummy",
        "google.calendar.client.id=dummy",
        "google.calendar.client.secret=dummy",
        "google.calendar.redirect.uris=https://dummy.com/success",
        "google.sheet.apps.name=dummy",
        "google.sheet.client.id=dummy",
        "google.sheet.client.secret=dummy",
        "google.sheet.redirect.uris=dummy",
        "aws.cognito.userPool.id=eu-west-3_vq2jlNjq7",
        "aws.cognito.userPool.domain=dummy",
        "aws.cognito.userPool.clientId=dummy",
        "aws.cognito.userPool.clientSecret=dummy",
        "aws.eventBridge.bus=dummy",
        "aws.sqs.mailboxUrl=dummy",
        "expressif.project.token=dummy",
        "feature.detector.api.key=dummy",
        "feature.detector.application.name=dummy",
        "fintecture.base.url=https://api-sandbox.fintecture.com",
        "swan.base.url=https://api.swan.io/sandbox-partner",
        "spring.datasource.url=" + postgresContainer.getJdbcUrl(),
        "spring.datasource.username=" + postgresContainer.getUsername(),
        "spring.datasource.password=" + postgresContainer.getPassword(),
        "spring.flyway.locations=classpath:/db/migration," + flywayTestdataPath);
  }

  public abstract int getServerPort();
}
