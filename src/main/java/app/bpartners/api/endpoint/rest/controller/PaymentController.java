package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.PaymentReqRestMapper;
import app.bpartners.api.endpoint.rest.mapper.PaymentUrlRestMapper;
import app.bpartners.api.endpoint.rest.model.PaymentReq;
import app.bpartners.api.endpoint.rest.model.PaymentUrl;
import app.bpartners.api.service.PaymentReqService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class PaymentController {
  private final PaymentReqRestMapper paymentReqMapper;
  private final PaymentUrlRestMapper paymentUrlMapper;
  private final PaymentReqService service;

  @PostMapping(value = "/paymentRequests")
  List<PaymentUrl> createPaymentReq(@RequestBody List<PaymentReq> paymentRequests) {
    List<app.bpartners.api.model.PaymentReq> domainPaymentReq = paymentRequests.stream()
        .map(paymentReqMapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
    return service.createPaymentReq(domainPaymentReq).stream()
        .map(paymentUrlMapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }
}
