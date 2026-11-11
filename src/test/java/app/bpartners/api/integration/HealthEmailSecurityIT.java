package app.bpartners.api.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.api.integration.conf.MockedThirdParties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class HealthEmailSecurityIT extends MockedThirdParties {

  @Test
  void anonymous_caller_cannot_send_a_health_email() throws Exception {
    var request =
        HttpRequest.newBuilder(
                URI.create("http://localhost:" + localPort + "/health/email?to=someone@mail.com"))
            .GET()
            .build();

    var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(403, response.statusCode());
  }
}
