package app.bpartners.api.unit.validator;

import static app.bpartners.api.integration.conf.TestUtils.assertThrowsBadRequestException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import app.bpartners.api.endpoint.rest.model.PaymentInitiation;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.validator.PaymentReqValidator;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Test;

@AllArgsConstructor
public class PaymentReqValidatorTest {
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
    assertThrowsBadRequestException("id is missing. ",
        () -> paymentReqValidator.accept(
            new PaymentInitiation()
                .id(null)
                .reference("payementRef")
                .label("paymentLabel")
                .redirectionStatusUrls(
                    new RedirectionStatusUrls()
                        .successUrl("success")
                        .failureUrl("failure")
                )
                .amount(1)
                .payerName("payerName")
                .payerEmail("payerEmail")
        ));
    assertThrowsBadRequestException("id is missing. amount is missing. ",
        () -> paymentReqValidator.accept(
            new PaymentInitiation()
                .id(null)
                .reference("payementRef")
                .label("paymentLabel")
                .redirectionStatusUrls(
                    new RedirectionStatusUrls()
                        .successUrl("success")
                        .failureUrl("failure")
                )
                .amount(null)
                .payerName("null")
                .payerEmail("payerEmail")
        ));
  }
}
