package app.bpartners.api.unit.validator;

import app.bpartners.api.endpoint.rest.model.PaymentInitiation;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.validator.PaymentInitValidator;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.integration.conf.TestUtils.assertThrowsBadRequestException;
import static app.bpartners.api.service.utils.EmailUtils.EMAIL_PATTERN;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PaymentReqValidatorTest {
  private final PaymentInitValidator subject = new PaymentInitValidator();

  private static PaymentInitiation correctPaymentInit() {
    return new PaymentInitiation()
        .id(randomUUID().toString())
        .reference("payementRef")
        .label("paymentLabel")
        .amount(1)
        .redirectionStatusUrls(
            new RedirectionStatusUrls()
                .successUrl("success")
                .failureUrl("failure")
        )
        .payerName("payerName")
        .payerEmail("payerEmail@email.com");
  }

  @Test
  void validate_paymentInitiation_ok() {
    assertDoesNotThrow(() -> subject.accept(correctPaymentInit()));
  }

  @Test
  void validate_invalid_paymentInitiation_ko() {
    assertThrowsBadRequestException("id is mandatory. "
            + "label is mandatory. "
            + "amount is mandatory. "
            + "payerName is mandatory. "
            + "payerEmail is mandatory. "
            + "redirectionStatusUrls is mandatory. ",
        () -> subject.accept(
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
    assertThrowsBadRequestException("id must be a valid UUID."
            + " redirectionStatusUrls.successUrl is mandatory. "
            + "redirectionStatusUrls.failureUrl is mandatory. ",
        () -> subject.accept(
            new PaymentInitiation()
                .id("fake_id")
                .reference("reference")
                .label("label")
                .redirectionStatusUrls(
                    new RedirectionStatusUrls()
                        .successUrl(null)
                        .failureUrl(null)
                )
                .amount(1)
                .payerName("payerName")
                .payerEmail("payerEmail@email.com")
        ));
  }

  @Test
  void validate_email_ko() {
    assertThrowsBadRequestException("payerEmail(badEmail) does not have valid email format."
            + " Pattern expected is " + EMAIL_PATTERN,
        () -> subject.accept(correctPaymentInit()
            .payerEmail("badEmail")
        ));
  }
}
