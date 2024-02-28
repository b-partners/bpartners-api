package app.bpartners.api.conf;

import app.bpartners.api.integration.conf.utils.TestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;

public class EnvConf {
  void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("server.port", TestUtils::findAvailableTcpPort);
    registry.add("sns.platform.arn", () -> "dummy");
    registry.add("admin.email", () -> "dummy");
    registry.add("sentry.dsn", () -> "dummy");
    registry.add("aws.bucket.name", () -> "bpartners");
    registry.add("aws.cognito.userPool.id", () -> "eu-west-3_vq2jlNjq7");
    registry.add("aws.cognito.userPool.domain", () -> "dummy");
    registry.add("aws.cognito.userPool.clientId", () -> "dummy");
    registry.add("aws.cognito.userPool.clientSecret", () -> "dummy");
    registry.add("aws.eventBridge.bus", () -> "dummy");
    registry.add("aws.sqs.mailboxUrl", () -> "dummy");
    registry.add("feature.detector.api.key", () -> "dummy");
    registry.add("feature.detector.application.name", () -> "dummy");
    registry.add("expressif.project.token", () -> "dummy");
    registry.add("ban.base.url", () -> "dummy");
    registry.add("bridge.base.url", () -> "dummy");
    registry.add("bridge.version", () -> "dummy");
    registry.add("bridge.client.id", () -> "dummy");
    registry.add("bridge.client.secret", () -> "dummy");
    registry.add("google.calendar.apps.name", () -> "dummy");
    registry.add("google.calendar.client.id", () -> "dummy");
    registry.add("google.calendar.client.secret", () -> "dummy");
    registry.add("google.calendar.redirect.uris", () -> "https://dummy.com/success");
    registry.add("google.sheet.apps.name", () -> "dummy");
    registry.add("google.sheet.client.id", () -> "dummy");
    registry.add("google.sheet.client.secret", () -> "dummy");
    registry.add("google.sheet.redirect.uris", () -> "https://dummy.com/success");
    registry.add("fintecture.base.url", () -> "https://api-sandbox.fintecture.com");
    registry.add("spring.flyway.locations", () -> "classpath:/db/migration,classpath:/db/testdata");
  }
}
