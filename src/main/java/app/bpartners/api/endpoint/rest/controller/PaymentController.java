package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.PaymentInitRestMapper;
import app.bpartners.api.endpoint.rest.model.PaymentInitiation;
import app.bpartners.api.endpoint.rest.model.PaymentRedirection;
import app.bpartners.api.service.PaymentInitiationService;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@Slf4j
public class PaymentController {
  private final PaymentInitRestMapper mapper;
  private final PaymentInitiationService initiationService;

  @PostMapping(value = "/webhooks/paymentStatus", consumes = "application/x-www-form-urlencoded")
  public void handlePaymentStatusChanges(
      @RequestParam("session_id") String sessionId,
      @RequestParam("status") String status,
      @RequestHeader("Signature") String signatureHeader) {
    // TODO: initiationService.verifySignature(signatureHeader, sessionId, status);
    initiationService.updatePaymentStatuses(Map.of(sessionId, status));
  }

  @PostMapping(value = "/accounts/{id}/paymentInitiations")
  List<PaymentRedirection> initiatePayments(
      @PathVariable(name = "id") String accountId,
      @RequestBody List<PaymentInitiation> paymentRequests) {
    List<app.bpartners.api.model.PaymentInitiation> domain =
        paymentRequests.stream().map(mapper::toDomain).toList();
    return initiationService.initiatePayments(accountId, domain).stream()
        .map(mapper::toRest)
        .toList();
  }
}
