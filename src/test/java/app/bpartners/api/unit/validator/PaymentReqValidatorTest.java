package app.bpartners.api.unit.validator;

import app.bpartners.api.endpoint.rest.model.PaymentInitiation;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.validator.PaymentReqValidator;
import org.junit.jupiter.api.Test;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsBadRequestException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PaymentReqValidatorTest {
  private final PaymentReqValidator paymentReqValidator = new PaymentReqValidator();

  @Test
  void validate_paymentInitiation_ok() {
    assertDoesNotThrow(() -> paymentReqValidator.accept(
        new PaymentInitiation()
            .id("paymentId")
            .reference("payementRef")
            .label("paymentLabel")
            .amount(1)
            .redirectionStatusUrls(
                new RedirectionStatusUrls()
                    .successUrl("success")
                    .failureUrl("failure")
            )
            .payerName("payerName")
            .payerEmail("payerEmail")
    ));
  }

  @Test
  void validate_invalid_paymentInitiation_ko() {
    assertThrowsBadRequestException("id is mandatory. "
            + "amount is mandatory. "
            + "payerName is mandatory. "
            + "payerEmail is mandatory. "
            + "redirectionStatusUrls is mandatory. ",
        () -> paymentReqValidator.accept(
            new PaymentInitiation()
                .id(null)
                .reference(null)
                .label(null)
                .redirectionStatusUrls(
                    null
                )
                .amount(null)
                .payerName(null)
                .payerEmail(null)
        ));
    assertThrowsBadRequestException("redirectionStatusUrls.successUrl is mandatory. "
            + "redirectionStatusUrls.failureUrl is mandatory. ",
        () -> paymentReqValidator.accept(
            new PaymentInitiation()
                .id("id")
                .reference("reference")
                .label("label")
                .redirectionStatusUrls(
                    new RedirectionStatusUrls()
                        .successUrl(null)
                        .failureUrl(null)
                )
                .amount(1)
                .payerName("payerName")
                .payerEmail("payerEmail")
        ));
  }
}
