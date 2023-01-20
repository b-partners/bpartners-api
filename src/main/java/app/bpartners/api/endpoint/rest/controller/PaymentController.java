package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.PaymentReqRestMapper;
import app.bpartners.api.endpoint.rest.mapper.PaymentUrlRestMapper;
import app.bpartners.api.endpoint.rest.model.PaymentInitiation;
import app.bpartners.api.endpoint.rest.model.PaymentRedirection;
import app.bpartners.api.service.PaymentInitiationService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class PaymentController {
  private final PaymentReqRestMapper paymentReqMapper;
  private final PaymentUrlRestMapper paymentUrlMapper;
  private final PaymentInitiationService initiationService;

  @PostMapping(value = "/accounts/{id}/paymentInitiations")
  List<PaymentRedirection> createPaymentReq(
      @PathVariable(name = "id") String accountId,
      @RequestBody List<PaymentInitiation> paymentRequests) {
    List<app.bpartners.api.model.PaymentInitiation> domainPaymentReq = paymentRequests.stream()
        .map(paymentReqMapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
    return initiationService.createPaymentReq(domainPaymentReq).stream()
        .map(paymentUrlMapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }
}
