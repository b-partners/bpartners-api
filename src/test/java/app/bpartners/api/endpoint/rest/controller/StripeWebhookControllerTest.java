package app.bpartners.api.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.service.subscription.StripeWebhookService;
import org.junit.jupiter.api.Test;

class StripeWebhookControllerTest {
  StripeWebhookService stripeWebhookServiceMock = mock();
  StripeWebhookController subject = new StripeWebhookController(stripeWebhookServiceMock);

  @Test
  void delegates_payload_and_signature_to_service_and_returns_ok() {
    var payload = "{\"type\":\"customer.subscription.updated\"}";
    var signature = "t=1,v1=abc";

    var actual = subject.handleStripeWebhook(payload, signature);

    assertEquals("ok", actual);
    verify(stripeWebhookServiceMock).handleEvent(payload, signature);
  }

  @Test
  void handles_missing_signature_header() {
    var payload = "{}";

    var actual = subject.handleStripeWebhook(payload, null);

    assertEquals("ok", actual);
    verify(stripeWebhookServiceMock).handleEvent(payload, null);
  }

  @Test
  void propagates_service_exception() {
    var payload = "{}";
    var signature = "bad";
    doThrow(new BadRequestException("Invalid Stripe webhook signature"))
        .when(stripeWebhookServiceMock)
        .handleEvent(payload, signature);

    assertThrows(BadRequestException.class, () -> subject.handleStripeWebhook(payload, signature));
  }
}
