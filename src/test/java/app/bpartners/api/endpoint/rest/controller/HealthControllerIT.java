package app.bpartners.api.endpoint.rest.controller;

import static app.bpartners.api.endpoint.rest.controller.health.PingController.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.api.PojaGenerated;
import app.bpartners.api.endpoint.rest.controller.health.HealthDbController;
import app.bpartners.api.endpoint.rest.controller.health.PingController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@PojaGenerated
class HealthControllerIT extends app.bpartners.api.integration.conf.MockedThirdParties {

  @Autowired PingController pingController;
  @Autowired HealthDbController healthDbController;

  @Test
  void ping() {
    assertEquals("pong", pingController.ping(SecurityContextHolder.createEmptyContext()));
  }

  @Test
  void can_read_from_dummy_table() {
    var responseEntity = healthDbController.dummyTable_should_not_be_empty();
    assertEquals(OK, responseEntity);
  }
}
