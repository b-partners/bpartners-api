package app.bpartners.api.integration;

import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsDomainApiException;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.controller.health.PingController;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.RestTemplate;

class RestTemplateIT extends MockedThirdParties {
  @Autowired RestTemplate restTemplate;
  @MockBean PingController pingController;

  @Test
  void rest_template_get_ok() {
    var response =
        restTemplate.getForEntity("http://localhost:" + localPort + "/ping", String.class);

    assertTrue(response.getStatusCode().is2xxSuccessful());
  }

  @Test
  void rest_template_5xx_error_is_handled_ok() {
    when(pingController.ping()).thenThrow(new ApiException(SERVER_EXCEPTION, "server error"));
    assertThrowsDomainApiException(
        "server exception with status 500 INTERNAL_SERVER_ERROR",
        () -> restTemplate.getForEntity("http://localhost:" + localPort + "/ping", String.class));
  }

  @Test
  void rest_template_4xx_error_is_handled_ok() {
    when(pingController.ping()).thenThrow(new BadRequestException("client error"));
    assertThrowsDomainApiException(
        "client exception with status 400 BAD_REQUEST",
        () -> restTemplate.getForEntity("http://localhost:" + localPort + "/ping", String.class));
  }
}
