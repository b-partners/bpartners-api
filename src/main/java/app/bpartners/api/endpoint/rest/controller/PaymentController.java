package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.service.payment.PaymentReceivedService;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@Slf4j
public class PaymentController {
  private final PaymentReceivedService receiptService;

  @PostMapping(value = "/webhooks/paymentStatus", consumes = "application/x-www-form-urlencoded")
  public void handlePaymentStatusChanges(
      @RequestParam("session_id") String sessionId,
      @RequestParam("status") String status,
      @RequestHeader("Signature") String signatureHeader) {
    // TODO: initiationService.verifySignature(signatureHeader, sessionId, status);
    receiptService.updatePaymentStatuses(Map.of(sessionId, status));
  }
}
