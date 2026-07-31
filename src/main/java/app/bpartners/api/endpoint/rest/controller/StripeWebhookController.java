package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.service.subscription.StripeWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StripeWebhookController {
  private final StripeWebhookService stripeWebhookService;

  @PostMapping("/webhooks/stripe")
  public String handleStripeWebhook(
      @RequestBody String payload,
      @RequestHeader(value = "Stripe-Signature", required = false) String signatureHeader) {
    stripeWebhookService.handleEvent(payload, signatureHeader);
    return "ok";
  }
}
