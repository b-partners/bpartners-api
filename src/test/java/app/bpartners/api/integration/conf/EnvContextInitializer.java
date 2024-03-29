package app.bpartners.api.integration.conf;

import static app.bpartners.api.integration.conf.utils.TestUtils.findAvailableTcpPort;
import static org.springframework.test.context.support.TestPropertySourceUtils.addInlinedPropertiesToEnvironment;

import lombok.Getter;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class EnvContextInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  private final DbContextInitializer dbContextInitializer;
  @Getter private int httpServerPort = -1;

  public EnvContextInitializer(DbContextInitializer dbContextInitializer) {
    this.dbContextInitializer = dbContextInitializer;
  }

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    httpServerPort = findAvailableTcpPort();
    var postgresContainer = dbContextInitializer.getPostgresContainer();
    addInlinedPropertiesToEnvironment(
        applicationContext,
        "server.port=" + httpServerPort,
        "sns.platform.arn=dummy",
        "admin.email=dummy",
        "bridge.client.id=dummy",
        "bridge.client.secret=dummy",
        "bridge.base.url=dummy",
        "bridge.version=dummy",
        "aws.cognito.userPool.id=eu-west-3_vq2jlNjq7",
        "aws.cognito.userPool.domain=dummy",
        "aws.cognito.userPool.clientId=dummy",
        "aws.cognito.userPool.clientSecret=dummy",
        "aws.eventBridge.bus=dummy",
        "aws.sqs.mailboxUrl=dummy",
        "feature.detector.api.key=dummy",
        "feature.detector.application.name=dummy",
        "ban.base.url=dummy",
        "expressif.project.token=dummy",
        "google.calendar.apps.name=dummy",
        "google.calendar.client.id=dummy",
        "google.calendar.client.secret=dummy",
        "google.calendar.redirect.uris=https://dummy.com/success",
        "google.sheet.apps.name=dummy",
        "google.sheet.client.id=dummy",
        "google.sheet.client.secret=dummy",
        "google.sheet.redirect.uris=https://dummy.com/success",
        "fintecture.base.url=https://api-sandbox.fintecture.com",
        "swan.base.url=https://api.swan.io/sandbox-partner",
        "spring.datasource.url=" + postgresContainer.getJdbcUrl(),
        "spring.datasource.username=" + postgresContainer.getUsername(),
        "spring.datasource.password=" + postgresContainer.getPassword(),
        "spring.flyway.locations=classpath:/db/migration,"
            + dbContextInitializer.getFlywayTestdataPath());
  }
}
