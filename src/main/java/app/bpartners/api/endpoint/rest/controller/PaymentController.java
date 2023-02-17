package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.PaymentInitRestMapper;
import app.bpartners.api.endpoint.rest.model.CreatePaymentInitiation;
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
  private final PaymentInitRestMapper mapper;
  private final PaymentInitiationService initiationService;

  @PostMapping(value = "/accounts/{id}/paymentInitiations")
  List<PaymentRedirection> initiatePayments(
      @PathVariable(name = "id") String accountId,
      @RequestBody List<CreatePaymentInitiation> paymentRequests) {
    List<app.bpartners.api.model.PaymentInitiation> domain = paymentRequests.stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
    return initiationService.initiatePayments(domain).stream()
        .map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }
}
