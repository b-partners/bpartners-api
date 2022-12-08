package app.bpartners.api.unit.validator;

import app.bpartners.api.endpoint.rest.model.CreateInvoiceRelaunch;
import app.bpartners.api.endpoint.rest.validator.CreateInvoiceRelaunchValidator;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.integration.conf.TestUtils.assertThrowsBadRequestException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class CreateInvoiceRelaunchValidatorTest {
  private static final String MAL_FORMED_HTML_EXCEPTION =
      "Your HTML syntax is malformed or you use other tags than these allowed :"
          + " a, b, blockquote, br, cite, code, dd, dl, dt, em, i, li, ol, p, pre, q, small, "
          + "span, strike, strong, sub, sup, u, ul";
  private final CreateInvoiceRelaunchValidator subject = new CreateInvoiceRelaunchValidator();

  CreateInvoiceRelaunch messageWithTextPlain() {
    return new CreateInvoiceRelaunch()
        .message("Text plain")
        .subject("Custom mail object");
  }

  CreateInvoiceRelaunch messageCorrectlyFormed() {
    return new CreateInvoiceRelaunch()
        .message("<p><strong><i><em>Hello</em></i></strong></p>");
  }

  CreateInvoiceRelaunch messageWithNotClosedTags() {
    return new CreateInvoiceRelaunch()
        .message("<p><p><i><em>Hello</em></i></p>");
  }

  CreateInvoiceRelaunch messageMalFormed() {
    return new CreateInvoiceRelaunch()
        .message("<p Hello </p>");
  }

  CreateInvoiceRelaunch messageWithBadTags() {
    return new CreateInvoiceRelaunch()
        .message("<img> Hello");
  }

  @Test
  void invoice_relaunch_has_allowed_tags_correctly_formed_ok() {
    assertDoesNotThrow(
        () -> subject.accept(messageCorrectlyFormed())
    );
  }

  @Test
  void invoice_relaunch_has_text_plain_ok() {
    assertDoesNotThrow(
        () -> subject.accept(messageWithTextPlain())
    );
  }

  @Test
  void invoice_relaunch_has_allowed_tags_mal_formed_ok() {
    assertDoesNotThrow(
        () -> subject.accept(messageWithNotClosedTags())
    );
  }

  @Test
  void invoice_relaunch_has_mal_formed_tags_ko() {
    assertThrowsBadRequestException(
        MAL_FORMED_HTML_EXCEPTION,
        () -> subject.accept(messageMalFormed())
    );
  }

  @Test
  void invoice_relaunch_has_bad_tags_ko() {
    assertThrowsBadRequestException(
        MAL_FORMED_HTML_EXCEPTION,
        () -> subject.accept(messageWithBadTags())
    );
  }
}
