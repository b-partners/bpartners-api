package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.PaymentInitiation;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.model.exception.BadRequestException;
import org.springframework.stereotype.Component;

@Component
public class PaymentReqValidator {
  public void accept(PaymentInitiation paymentInitiation) {
    if (paymentInitiation.getId() == null) {
      throw new BadRequestException("id should not be null");
    }
    if (paymentInitiation.getLabel() == null) {
      throw new BadRequestException("label should not be null");
    }
    if (paymentInitiation.getReference() == null) {
      throw new BadRequestException("reference should not be null");
    }
    if (paymentInitiation.getAmount() == null) {
      throw new BadRequestException("amount should not be null");
    }
    if (paymentInitiation.getPayerName() == null) {
      throw new BadRequestException("payerName should not be null");
    }
    if (paymentInitiation.getPayerEmail() == null) {
      throw new BadRequestException("payerEmail should not be null");
    }

    RedirectionStatusUrls redirectionStatusUrls = paymentInitiation.getRedirectionStatusUrls();

    if (redirectionStatusUrls == null) {
      throw new BadRequestException("redirectionStatusUrls should not be null");
    }
    if (redirectionStatusUrls.getSuccessUrl() == null) {
      throw new BadRequestException("successUrl should not be null");
    }
    if (redirectionStatusUrls.getFailureUrl() == null) {
      throw new BadRequestException("failureUrl should not be null");
    }

  }
}
